package me.hanhyur.gatewell.scanner

import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Component
class GitHubCodeScanner {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val objectMapper = jacksonObjectMapper()

    private val secretPatterns = listOf(
        SecretPattern("AWS Access Key", Regex("AKIA[0-9A-Z]{16}")),
        SecretPattern("AWS Secret Key", Regex("(?i)aws_secret_access_key\\s*=\\s*[A-Za-z0-9/+=]{40}")),
        SecretPattern("GitHub Token", Regex("gh[ps]_[A-Za-z0-9_]{36,}")),
        SecretPattern("Generic API Key", Regex("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[\"']?[A-Za-z0-9_\\-]{20,}[\"']?")),
        SecretPattern("Generic Secret", Regex("(?i)(secret|password|passwd|pwd)\\s*[:=]\\s*[\"'][^\"']{8,}[\"']")),
        SecretPattern("Private Key", Regex("-----BEGIN (RSA |EC |DSA )?PRIVATE KEY-----")),
        SecretPattern("JWT Token", Regex("eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}")),
        SecretPattern("Slack Token", Regex("xox[baprs]-[0-9]{10,}-[A-Za-z0-9]+")),
        SecretPattern("Stripe Key", Regex("sk_live_[0-9a-zA-Z]{24,}")),
        SecretPattern("Google API Key", Regex("AIza[0-9A-Za-z\\-_]{35}")),
        SecretPattern("Database URL with password", Regex("(?i)(postgres|mysql|mongodb)://[^:]+:[^@]+@")),
    )

    private val sensitiveFiles = listOf(
        ".env", ".env.local", ".env.production",
        "credentials.json", "service-account.json",
        "id_rsa", "id_ed25519", ".pem",
        "docker-compose.yml",
    )

    private val codeVulnPatterns = listOf(
        CodeVulnPattern(
            "SQL Injection",
            Regex("(?i)(execute|query)\\s*\\(\\s*[\"'].*\\$\\{|\\+\\s*\\w+\\s*\\+"),
            "String concatenation in SQL query. Use parameterized queries.",
            listOf("*.java", "*.kt", "*.py", "*.js", "*.ts"),
        ),
        CodeVulnPattern(
            "Eval Usage",
            Regex("\\beval\\s*\\("),
            "eval() executes arbitrary code. Remove or replace with safe alternatives.",
            listOf("*.js", "*.ts", "*.py"),
        ),
        CodeVulnPattern(
            "Dangerous innerHTML",
            Regex("(?i)innerhtml\\s*=|dangerouslysetinnerhtml"),
            "Direct HTML injection. Use text content or sanitize input.",
            listOf("*.js", "*.ts", "*.jsx", "*.tsx"),
        ),
        CodeVulnPattern(
            "Hardcoded CORS wildcard",
            Regex("(?i)access-control-allow-origin.*\\*|cors.*origin.*\\*"),
            "CORS wildcard allows any site to access the API.",
            listOf("*.java", "*.kt", "*.py", "*.js", "*.ts", "*.go"),
        ),
        CodeVulnPattern(
            "Disabled SSL verification",
            Regex("(?i)(verify\\s*=\\s*false|ssl_verify.*false|reject_?unauthorized.*false|insecure_?skip_?verify)"),
            "SSL verification disabled. Man-in-the-middle attacks possible.",
            listOf("*"),
        ),
    )

    fun scan(repoUrl: String): ScanReport {
        val (owner, repo) = parseRepoUrl(repoUrl)
        val findings = mutableListOf<ScanFinding>()

        val tree = fetchRepoTree(owner, repo)
        if (tree == null) {
            return ScanReport(
                url = repoUrl,
                reachable = false,
                findings = listOf(
                    ScanFinding(
                        severity = ScanSeverity.HIGH,
                        category = ScanCategory.CODE_VULNERABILITY,
                        code = "REPO_INACCESSIBLE",
                        title = "Repository not accessible",
                        detail = "Could not access the GitHub repository. Private repositories are not supported — only public repos can be scanned. Please check the URL and try again.",
                        evidence = "GitHub API returned error for $owner/$repo",
                    )
                ),
            )
        }

        val filePaths = tree.map { it.path }

        checkSensitiveFiles(filePaths, owner, repo, findings)
        scanFilesForSecrets(tree, owner, repo, findings)
        scanFilesForVulnerabilities(tree, owner, repo, findings)

        return ScanReport(url = repoUrl, reachable = true, findings = findings.sortedBy { it.severity.ordinal })
    }

    private fun checkSensitiveFiles(paths: List<String>, owner: String, repo: String, findings: MutableList<ScanFinding>) {
        for (path in paths) {
            val fileName = path.substringAfterLast("/")
            val matchedSensitive = sensitiveFiles.firstOrNull { fileName == it || fileName.endsWith(it) }

            if (matchedSensitive != null) {
                val severity = when {
                    matchedSensitive.contains("env") -> ScanSeverity.CRITICAL
                    matchedSensitive.contains("credential") || matchedSensitive.contains("service-account") -> ScanSeverity.CRITICAL
                    matchedSensitive.endsWith("_rsa") || matchedSensitive.endsWith(".pem") -> ScanSeverity.CRITICAL
                    else -> ScanSeverity.MEDIUM
                }

                findings += ScanFinding(
                    severity = severity,
                    category = ScanCategory.SENSITIVE_FILE_EXPOSED,
                    code = "SENSITIVE_FILE_${matchedSensitive.replace(".", "_").uppercase()}",
                    title = "Sensitive file committed: $path",
                    detail = "File '$matchedSensitive' should not be in the repository. It may contain secrets or private keys.",
                    evidence = "Found in $owner/$repo at path: $path",
                )
            }
        }
    }

    private fun scanFilesForSecrets(tree: List<TreeEntry>, owner: String, repo: String, findings: MutableList<ScanFinding>) {
        val scanExtensions = setOf("java", "kt", "py", "js", "ts", "jsx", "tsx", "go", "rb", "yml", "yaml", "json", "properties", "cfg", "ini", "toml")
        val filesToScan = tree
            .filter { it.type == "blob" && it.size < 50000 }
            .filter { scanExtensions.any { ext -> it.path.endsWith(".$ext") } }
            .take(100)

        for (file in filesToScan) {
            val content = fetchFileContent(owner, repo, file.path) ?: continue

            for (pattern in secretPatterns) {
                val match = pattern.regex.find(content)
                if (match != null) {
                    val redacted = match.value.take(8) + "..." + match.value.takeLast(4)
                    findings += ScanFinding(
                        severity = ScanSeverity.CRITICAL,
                        category = ScanCategory.HARDCODED_SECRET,
                        code = "SECRET_${pattern.name.uppercase().replace(" ", "_")}",
                        title = "${pattern.name} found in ${file.path}",
                        detail = "Hardcoded secret detected. Rotate this credential immediately and use environment variables instead.",
                        evidence = "Pattern match: $redacted (in ${file.path})",
                    )
                    break
                }
            }
        }
    }

    private fun scanFilesForVulnerabilities(tree: List<TreeEntry>, owner: String, repo: String, findings: MutableList<ScanFinding>) {
        val filesToScan = tree
            .filter { it.type == "blob" && it.size < 50000 }
            .filter { entry -> codeVulnPatterns.any { p -> p.fileGlobs.any { g -> g == "*" || entry.path.endsWith(g.removePrefix("*")) } } }
            .take(80)

        for (file in filesToScan) {
            val content = fetchFileContent(owner, repo, file.path) ?: continue

            for (pattern in codeVulnPatterns) {
                if (!pattern.fileGlobs.any { it == "*" || file.path.endsWith(it.removePrefix("*")) }) continue

                val match = pattern.regex.find(content)
                if (match != null) {
                    findings += ScanFinding(
                        severity = ScanSeverity.HIGH,
                        category = ScanCategory.CODE_VULNERABILITY,
                        code = "VULN_${pattern.name.uppercase().replace(" ", "_")}",
                        title = "${pattern.name} in ${file.path}",
                        detail = pattern.detail,
                        evidence = "Line: ${match.value.take(80)}",
                    )
                }
            }
        }
    }

    private fun fetchRepoTree(owner: String, repo: String): List<TreeEntry>? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$owner/$repo/git/trees/HEAD?recursive=1"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Gatewell-Scanner")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build()

        val response = runCatching {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrNull() ?: return null

        if (response.statusCode() != 200) return null

        val tree = objectMapper.readValue<Map<String, Any>>(response.body())
        @Suppress("UNCHECKED_CAST")
        val entries = tree["tree"] as? List<Map<String, Any>> ?: return null

        return entries.map { entry ->
            TreeEntry(
                path = entry["path"] as? String ?: "",
                type = entry["type"] as? String ?: "",
                size = (entry["size"] as? Number)?.toLong() ?: 0,
            )
        }
    }

    private fun fetchFileContent(owner: String, repo: String, path: String): String? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://raw.githubusercontent.com/$owner/$repo/HEAD/$path"))
            .header("User-Agent", "Gatewell-Scanner")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        return runCatching {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) response.body() else null
        }.getOrNull()
    }

    private fun parseRepoUrl(url: String): Pair<String, String> {
        val cleaned = url
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix(".git")

        val parts = cleaned.split("/")
        require(parts.size >= 2) { "Invalid GitHub repository URL. Expected format: owner/repo or https://github.com/owner/repo" }
        return parts[0] to parts[1]
    }
}

private data class SecretPattern(val name: String, val regex: Regex)
private data class CodeVulnPattern(val name: String, val regex: Regex, val detail: String, val fileGlobs: List<String>)
private data class TreeEntry(val path: String, val type: String, val size: Long)
