package me.hanhyur.gatewell.scanner

import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class UrlSecurityScanner {

    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun scan(targetUrl: String): ScanReport {
        val url = normalizeUrl(targetUrl)
        val findings = mutableListOf<ScanFinding>()

        val mainResponse = fetchSafely(url)
        if (mainResponse == null) {
            return ScanReport(
                url = url,
                reachable = false,
                findings = listOf(
                    ScanFinding(
                        severity = ScanSeverity.CRITICAL,
                        category = ScanCategory.SSL_TLS,
                        code = "UNREACHABLE",
                        title = "Target unreachable",
                        detail = "Could not connect to $url. The site may be down or blocking requests.",
                        evidence = "Connection failed",
                    )
                ),
            )
        }

        if (isBotProtectionPage(mainResponse)) {
            return ScanReport(
                url = url,
                reachable = true,
                findings = listOf(
                    ScanFinding(
                        severity = ScanSeverity.INFO,
                        category = ScanCategory.AUTHENTICATION,
                        code = "BOT_PROTECTION_DETECTED",
                        title = "Bot protection detected — scan results may be inaccurate",
                        detail = "This site uses Cloudflare, AWS WAF, or similar bot protection. " +
                            "The scanner received a challenge page instead of the actual site content. " +
                            "Results cannot be trusted. This scanner is designed for sites without enterprise-grade bot protection " +
                            "(side projects, startups, vibe-coded apps).",
                        evidence = "Response contains bot protection signatures (challenges.cloudflare.com, cf-mitigated, captcha)",
                    )
                ),
            )
        }

        checkSecurityHeaders(mainResponse, findings)
        checkCors(url, findings)
        checkCookieSecurity(mainResponse, findings)
        checkInformationLeakage(mainResponse, findings)
        checkExposedPaths(url, findings)
        checkErrorHandling(url, findings)
        checkSsl(url, findings)

        return ScanReport(url = url, reachable = true, findings = findings.sortedBy { it.severity.ordinal })
    }

    private fun isBotProtectionPage(response: HttpResponseData): Boolean {
        val body = response.body.lowercase()
        val headers = response.headers

        val cloudflareChallenge = body.contains("challenges.cloudflare.com")
            || body.contains("cf-challenge")
            || body.contains("cf_chl_opt")
            || headers["cf-mitigated"] != null
            || (headers["server"]?.lowercase()?.contains("cloudflare") == true && response.statusCode == 403)

        val genericChallenge = body.contains("captcha")
            && body.contains("challenge")
            && response.statusCode in listOf(403, 503)

        val awsWaf = headers["x-amzn-waf-action"] != null
            || (response.statusCode == 403 && body.contains("automated access"))

        return cloudflareChallenge || genericChallenge || awsWaf
    }

    private fun checkSecurityHeaders(response: HttpResponseData, findings: MutableList<ScanFinding>) {
        val headers = response.headers

        val required = mapOf(
            "strict-transport-security" to Pair("MISSING_HSTS", "Strict-Transport-Security header missing. Site is vulnerable to protocol downgrade attacks."),
            "x-content-type-options" to Pair("MISSING_XCTO", "X-Content-Type-Options header missing. Browser may MIME-sniff responses, enabling XSS."),
            "x-frame-options" to Pair("MISSING_XFO", "X-Frame-Options header missing. Site is vulnerable to clickjacking attacks."),
            "referrer-policy" to Pair("MISSING_REFERRER_POLICY", "Referrer-Policy header missing. Referrer data may leak to third parties."),
        )

        for ((header, info) in required) {
            if (!headers.containsKey(header)) {
                findings += ScanFinding(
                    severity = if (header == "strict-transport-security") ScanSeverity.HIGH else ScanSeverity.MEDIUM,
                    category = ScanCategory.MISSING_SECURITY_HEADER,
                    code = info.first,
                    title = "Missing ${header.uppercase()}",
                    detail = info.second,
                    evidence = "Header not present in response",
                )
            }
        }

        val csp = headers["content-security-policy"]
        if (csp == null) {
            findings += ScanFinding(
                severity = ScanSeverity.HIGH,
                category = ScanCategory.MISSING_SECURITY_HEADER,
                code = "MISSING_CSP",
                title = "Missing Content-Security-Policy",
                detail = "No CSP header. Site is vulnerable to XSS and data injection attacks.",
                evidence = "Header not present in response",
            )
        } else if (csp.contains("unsafe-inline") || csp.contains("unsafe-eval")) {
            findings += ScanFinding(
                severity = ScanSeverity.MEDIUM,
                category = ScanCategory.MISSING_SECURITY_HEADER,
                code = "WEAK_CSP",
                title = "Weak Content-Security-Policy",
                detail = "CSP contains 'unsafe-inline' or 'unsafe-eval', reducing XSS protection.",
                evidence = "CSP: $csp",
            )
        }

        val permissionsPolicy = headers["permissions-policy"]
        if (permissionsPolicy == null) {
            findings += ScanFinding(
                severity = ScanSeverity.LOW,
                category = ScanCategory.MISSING_SECURITY_HEADER,
                code = "MISSING_PERMISSIONS_POLICY",
                title = "Missing Permissions-Policy",
                detail = "No Permissions-Policy header. Browser features like camera/microphone are not restricted.",
                evidence = "Header not present in response",
            )
        }
    }

    private fun checkCors(url: String, findings: MutableList<ScanFinding>) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Origin", "https://evil-attacker.com")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        val response = runCatching {
            client.send(request, HttpResponse.BodyHandlers.discarding())
        }.getOrNull() ?: return

        val acao = response.headers().firstValue("access-control-allow-origin").orElse(null)
        val acac = response.headers().firstValue("access-control-allow-credentials").orElse(null)

        if (acao == "*") {
            if (acac == "true") {
                findings += ScanFinding(
                    severity = ScanSeverity.CRITICAL,
                    category = ScanCategory.CORS_MISCONFIGURATION,
                    code = "CORS_WILDCARD_WITH_CREDENTIALS",
                    title = "CORS wildcard with credentials",
                    detail = "Access-Control-Allow-Origin: * combined with credentials. Any site can steal authenticated data.",
                    evidence = "ACAO: *, ACAC: true",
                )
            } else {
                findings += ScanFinding(
                    severity = ScanSeverity.MEDIUM,
                    category = ScanCategory.CORS_MISCONFIGURATION,
                    code = "CORS_WILDCARD",
                    title = "CORS allows all origins",
                    detail = "Access-Control-Allow-Origin: * allows any website to read responses.",
                    evidence = "ACAO: *",
                )
            }
        } else if (acao == "https://evil-attacker.com") {
            findings += ScanFinding(
                severity = ScanSeverity.CRITICAL,
                category = ScanCategory.CORS_MISCONFIGURATION,
                code = "CORS_REFLECTS_ORIGIN",
                title = "CORS reflects arbitrary origin",
                detail = "Server reflects any Origin header back. This is equivalent to a wildcard and allows cross-origin data theft.",
                evidence = "Sent Origin: https://evil-attacker.com, got ACAO: https://evil-attacker.com",
            )
        }
    }

    private fun checkCookieSecurity(response: HttpResponseData, findings: MutableList<ScanFinding>) {
        val setCookies = response.setCookieHeaders
        for (cookie in setCookies) {
            val lower = cookie.lowercase()
            val name = cookie.substringBefore("=").trim()

            if (!lower.contains("httponly")) {
                findings += ScanFinding(
                    severity = ScanSeverity.MEDIUM,
                    category = ScanCategory.COOKIE_SECURITY,
                    code = "COOKIE_NO_HTTPONLY",
                    title = "Cookie '$name' missing HttpOnly flag",
                    detail = "Cookie accessible via JavaScript. XSS attacks can steal this cookie.",
                    evidence = "Set-Cookie: ${cookie.take(80)}...",
                )
            }
            if (!lower.contains("secure")) {
                findings += ScanFinding(
                    severity = ScanSeverity.MEDIUM,
                    category = ScanCategory.COOKIE_SECURITY,
                    code = "COOKIE_NO_SECURE",
                    title = "Cookie '$name' missing Secure flag",
                    detail = "Cookie can be sent over unencrypted HTTP connections.",
                    evidence = "Set-Cookie: ${cookie.take(80)}...",
                )
            }
            if (!lower.contains("samesite")) {
                findings += ScanFinding(
                    severity = ScanSeverity.LOW,
                    category = ScanCategory.COOKIE_SECURITY,
                    code = "COOKIE_NO_SAMESITE",
                    title = "Cookie '$name' missing SameSite attribute",
                    detail = "Cookie may be sent on cross-site requests, enabling CSRF attacks.",
                    evidence = "Set-Cookie: ${cookie.take(80)}...",
                )
            }
        }
    }

    private fun checkInformationLeakage(response: HttpResponseData, findings: MutableList<ScanFinding>) {
        val server = response.headers["server"]
        if (server != null && server.matches(Regex(".*(Apache|nginx|IIS|Express|Jetty|Tomcat|Kestrel).*", RegexOption.IGNORE_CASE))) {
            findings += ScanFinding(
                severity = ScanSeverity.LOW,
                category = ScanCategory.INFORMATION_LEAKAGE,
                code = "SERVER_HEADER_EXPOSED",
                title = "Server technology exposed",
                detail = "Server header reveals technology stack, aiding targeted attacks.",
                evidence = "Server: $server",
            )
        }

        val powered = response.headers["x-powered-by"]
        if (powered != null) {
            findings += ScanFinding(
                severity = ScanSeverity.LOW,
                category = ScanCategory.INFORMATION_LEAKAGE,
                code = "POWERED_BY_EXPOSED",
                title = "X-Powered-By header exposed",
                detail = "Reveals framework/language version, aiding targeted attacks.",
                evidence = "X-Powered-By: $powered",
            )
        }
    }

    private fun checkExposedPaths(baseUrl: String, findings: MutableList<ScanFinding>) {
        val sensitivePaths = listOf(
            Pair("/.env", "Environment file may contain secrets (API keys, DB passwords)"),
            Pair("/.git/config", "Git repository exposed. Attackers can download source code"),
            Pair("/.git/HEAD", "Git repository exposed. Attackers can download source code"),
            Pair("/wp-admin/", "WordPress admin panel exposed"),
            Pair("/phpinfo.php", "PHP info page exposes server configuration"),
            Pair("/server-status", "Apache server status exposed"),
            Pair("/actuator", "Spring Boot Actuator endpoints may expose internal data"),
            Pair("/actuator/env", "Spring Boot environment may contain secrets"),
            Pair("/debug", "Debug endpoint exposed in production"),
            Pair("/api-docs", "API documentation exposed without authentication"),
            Pair("/graphql", "GraphQL endpoint exposed (may allow introspection)"),
            Pair("/admin", "Admin panel accessible"),
            Pair("/.well-known/security.txt", null),
        )

        for ((path, description) in sensitivePaths) {
            val resp = fetchSafely("$baseUrl$path") ?: continue

            if (resp.statusCode in 200..299 && description != null) {
                val severity = when {
                    path.contains(".env") || path.contains(".git") -> ScanSeverity.CRITICAL
                    path.contains("actuator/env") || path.contains("phpinfo") -> ScanSeverity.HIGH
                    path.contains("admin") || path.contains("debug") -> ScanSeverity.MEDIUM
                    else -> ScanSeverity.LOW
                }

                findings += ScanFinding(
                    severity = severity,
                    category = ScanCategory.EXPOSED_SENSITIVE_PATH,
                    code = "EXPOSED_PATH_${path.replace("/", "_").replace(".", "_").uppercase().trim('_')}",
                    title = "Sensitive path accessible: $path",
                    detail = description,
                    evidence = "GET $baseUrl$path → HTTP ${resp.statusCode}",
                )
            }
        }
    }

    private fun checkErrorHandling(baseUrl: String, findings: MutableList<ScanFinding>) {
        val probeUrls = listOf(
            "$baseUrl/nonexistent-path-" + System.currentTimeMillis(),
            "$baseUrl/api/v1/' OR '1'='1",
            "$baseUrl/<script>alert(1)</script>",
        )

        for (probeUrl in probeUrls) {
            val resp = fetchSafely(probeUrl) ?: continue
            val body = resp.body.lowercase()

            val stackTracePatterns = listOf(
                "stacktrace", "stack trace", "caused by:", "at com.", "at org.",
                "exception in", "traceback", "file \"", "line \\d+",
                "syntaxerror", "typeerror", "nullpointerexception",
            )

            if (stackTracePatterns.any { body.contains(it) }) {
                findings += ScanFinding(
                    severity = ScanSeverity.HIGH,
                    category = ScanCategory.ERROR_HANDLING,
                    code = "STACK_TRACE_EXPOSED",
                    title = "Stack trace exposed in error response",
                    detail = "Error responses contain technical stack traces. Attackers can learn internal structure, frameworks, and file paths.",
                    evidence = "Probe URL revealed stack trace information",
                )
                break
            }

            if (body.contains("<script>alert(1)</script>")) {
                findings += ScanFinding(
                    severity = ScanSeverity.CRITICAL,
                    category = ScanCategory.ERROR_HANDLING,
                    code = "REFLECTED_XSS",
                    title = "Reflected XSS detected",
                    detail = "User input is reflected in error pages without sanitization. Attackers can execute arbitrary JavaScript.",
                    evidence = "Script tag reflected in response body",
                )
                break
            }
        }
    }

    private fun checkSsl(url: String, findings: MutableList<ScanFinding>) {
        if (url.startsWith("http://")) {
            findings += ScanFinding(
                severity = ScanSeverity.HIGH,
                category = ScanCategory.SSL_TLS,
                code = "NO_HTTPS",
                title = "Site uses HTTP instead of HTTPS",
                detail = "All traffic is unencrypted. Attackers on the network can read and modify data in transit.",
                evidence = "URL scheme: http://",
            )
        }

        val httpUrl = url.replace("https://", "http://")
        if (url.startsWith("https://")) {
            val resp = fetchSafely(httpUrl)
            if (resp != null && resp.statusCode in 200..299) {
                findings += ScanFinding(
                    severity = ScanSeverity.MEDIUM,
                    category = ScanCategory.SSL_TLS,
                    code = "HTTP_ACCESSIBLE",
                    title = "HTTP version still accessible",
                    detail = "Site responds on HTTP without redirecting to HTTPS. Users may accidentally use the unencrypted version.",
                    evidence = "GET $httpUrl → HTTP ${resp.statusCode} (no redirect to HTTPS)",
                )
            }
        }
    }

    private fun fetchSafely(url: String): HttpResponseData? {
        return runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            HttpResponseData(
                statusCode = response.statusCode(),
                headers = response.headers().map().mapValues { it.value.firstOrNull() ?: "" }
                    .mapKeys { it.key.lowercase() },
                body = response.body().take(5000),
                setCookieHeaders = response.headers().allValues("set-cookie"),
            )
        }.getOrNull()
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        val normalized = when {
            trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "https://")
            trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
        validateNotInternal(normalized)
        return normalized
    }

    private fun validateNotInternal(url: String) {
        val uri = URI.create(url)
        val host = uri.host ?: throw IllegalArgumentException("Invalid URL: no host")

        val blocked = listOf(
            "localhost", "127.0.0.1", "0.0.0.0", "::1",
            "metadata.google.internal", "169.254.169.254",
        )
        require(host !in blocked) { "Scanning internal addresses is not allowed" }

        val ip = runCatching { java.net.InetAddress.getByName(host) }.getOrNull()
        if (ip != null) {
            require(!ip.isLoopbackAddress) { "Scanning loopback addresses is not allowed" }
            require(!ip.isSiteLocalAddress) { "Scanning private network addresses is not allowed" }
            require(!ip.isLinkLocalAddress) { "Scanning link-local addresses is not allowed" }
        }
    }
}

data class HttpResponseData(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String,
    val setCookieHeaders: List<String>,
)

data class ScanReport(
    val url: String,
    val reachable: Boolean,
    val findings: List<ScanFinding>,
) {
    val criticalCount get() = findings.count { it.severity == ScanSeverity.CRITICAL }
    val highCount get() = findings.count { it.severity == ScanSeverity.HIGH }
    val mediumCount get() = findings.count { it.severity == ScanSeverity.MEDIUM }
    val lowCount get() = findings.count { it.severity == ScanSeverity.LOW }

    val decision: String
        get() = when {
            criticalCount > 0 || highCount >= 2 -> "BLOCK"
            highCount > 0 || mediumCount >= 3 -> "CAUTION"
            else -> "ALLOW"
        }
}
