package me.hanhyur.gatewell.scanner

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/scan")
class ScanController(
    private val urlSecurityScanner: UrlSecurityScanner,
    private val gitHubCodeScanner: GitHubCodeScanner,
) {

    @PostMapping("/url")
    fun scanUrl(@Valid @RequestBody request: UrlScanRequest): ResponseEntity<ScanResponse> {
        val report = urlSecurityScanner.scan(request.url)
        return ResponseEntity.ok(ScanResponse.from(report, "url"))
    }

    @PostMapping("/github")
    fun scanGitHub(@Valid @RequestBody request: GitHubScanRequest): ResponseEntity<ScanResponse> {
        val report = gitHubCodeScanner.scan(request.repoUrl)
        return ResponseEntity.ok(ScanResponse.from(report, "github"))
    }
}

data class UrlScanRequest(
    @field:NotBlank
    val url: String,
)

data class GitHubScanRequest(
    @field:NotBlank
    val repoUrl: String,
)

data class ScanResponse(
    val scanType: String,
    val target: String,
    val reachable: Boolean,
    val decision: String,
    val summary: ScanSummary,
    val findings: List<ScanFindingResponse>,
) {
    companion object {
        fun from(report: ScanReport, scanType: String): ScanResponse {
            return ScanResponse(
                scanType = scanType,
                target = report.url,
                reachable = report.reachable,
                decision = report.decision,
                summary = ScanSummary(
                    total = report.findings.size,
                    critical = report.criticalCount,
                    high = report.highCount,
                    medium = report.mediumCount,
                    low = report.lowCount,
                    info = report.findings.count { it.severity == ScanSeverity.INFO },
                    categories = report.findings.map { it.category.name }.distinct().sorted(),
                ),
                findings = report.findings.map { f ->
                    ScanFindingResponse(
                        severity = f.severity.name,
                        category = f.category.name,
                        code = f.code,
                        title = f.title,
                        detail = f.detail,
                        evidence = f.evidence,
                    )
                },
            )
        }
    }
}

data class ScanSummary(
    val total: Int,
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val info: Int,
    val categories: List<String>,
)

data class ScanFindingResponse(
    val severity: String,
    val category: String,
    val code: String,
    val title: String,
    val detail: String,
    val evidence: String,
)
