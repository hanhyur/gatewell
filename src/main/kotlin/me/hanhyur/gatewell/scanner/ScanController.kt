package me.hanhyur.gatewell.scanner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import me.hanhyur.gatewell.scanner.persistence.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/scan")
class ScanController(
    private val urlSecurityScanner: UrlSecurityScanner,
    private val gitHubCodeScanner: GitHubCodeScanner,
    private val scanResultRepository: ScanResultRepository,
    private val scanUsageRepository: ScanUsageRepository,
    private val emailLeadRepository: EmailLeadRepository,
) {
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val FREE_DAILY_LIMIT = 3
        private const val EMAIL_BONUS = 3
    }

    @PostMapping("/url")
    fun scanUrl(
        @Valid @RequestBody request: UrlScanRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val ip = extractClientIp(httpRequest)
        val limitCheck = checkLimit(ip)
        if (limitCheck != null) return limitCheck

        val report = urlSecurityScanner.scan(request.url)
        val response = ScanResponse.from(report, "url")
        val saved = saveResult(response)
        incrementUsage(ip)

        val usage = getUsage(ip)
        return ResponseEntity.ok(response.copy(
            id = saved.id.toString(),
            remainingScans = maxScansFor(ip) - usage.scanCount,
        ))
    }

    @PostMapping("/github")
    fun scanGitHub(
        @Valid @RequestBody request: GitHubScanRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val ip = extractClientIp(httpRequest)
        val limitCheck = checkLimit(ip)
        if (limitCheck != null) return limitCheck

        val report = gitHubCodeScanner.scan(request.repoUrl)
        val response = ScanResponse.from(report, "github")
        val saved = saveResult(response)
        incrementUsage(ip)

        val usage = getUsage(ip)
        return ResponseEntity.ok(response.copy(
            id = saved.id.toString(),
            remainingScans = maxScansFor(ip) - usage.scanCount,
        ))
    }

    @GetMapping("/results/{id}")
    fun getResult(@PathVariable id: String): ResponseEntity<ScanResponse> {
        val entity = scanResultRepository.findByIdOrNull(UUID.fromString(id))
            ?: return ResponseEntity.notFound().build()

        val findings: List<ScanFindingResponse> = objectMapper.readValue(entity.findingsJson)

        return ResponseEntity.ok(
            ScanResponse(
                id = entity.id.toString(),
                scanType = entity.scanType,
                target = entity.target,
                reachable = entity.reachable,
                decision = entity.decision,
                summary = ScanSummary(
                    total = entity.totalFindings,
                    critical = entity.criticalCount,
                    high = entity.highCount,
                    medium = entity.mediumCount,
                    low = entity.lowCount,
                    info = entity.infoCount,
                    categories = entity.categories.split(",").filter { it.isNotBlank() },
                ),
                findings = findings,
                createdAt = entity.createdAt.toString(),
            )
        )
    }

    @GetMapping("/remaining")
    fun remaining(httpRequest: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val ip = extractClientIp(httpRequest)
        val usage = getUsage(ip)
        val max = maxScansFor(ip)
        return ResponseEntity.ok(mapOf(
            "remaining" to (max - usage.scanCount).coerceAtLeast(0),
            "limit" to max,
            "used" to usage.scanCount,
        ))
    }

    @PostMapping("/register-email")
    fun registerEmail(
        @Valid @RequestBody request: EmailRegistrationRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> {
        val ip = extractClientIp(httpRequest)

        if (!emailLeadRepository.existsByEmail(request.email)) {
            emailLeadRepository.save(EmailLeadEntity(email = request.email, clientIp = ip))
        }

        val usage = getUsage(ip)
        val max = maxScansFor(ip)
        return ResponseEntity.ok(mapOf(
            "message" to "Email registered. You have ${EMAIL_BONUS} additional scans today.",
            "remaining" to (max - usage.scanCount).coerceAtLeast(0),
            "limit" to max,
        ))
    }

    private fun checkLimit(ip: String): ResponseEntity<Any>? {
        val usage = getUsage(ip)
        val max = maxScansFor(ip)
        if (usage.scanCount >= max) {
            val hasEmail = emailLeadRepository.existsByEmail(ip).not()
            return ResponseEntity.status(429).body(mapOf(
                "error" to "Daily scan limit reached",
                "limit" to max,
                "used" to usage.scanCount,
                "canUnlockWithEmail" to (max == FREE_DAILY_LIMIT),
            ))
        }
        return null
    }

    private fun getUsage(ip: String): ScanUsageEntity {
        return scanUsageRepository.findByClientIpAndScanDate(ip, LocalDate.now())
            ?: ScanUsageEntity(clientIp = ip, scanCount = 0)
    }

    private fun incrementUsage(ip: String) {
        val today = LocalDate.now()
        val existing = scanUsageRepository.findByClientIpAndScanDate(ip, today)
        if (existing != null) {
            scanUsageRepository.save(ScanUsageEntity(
                id = existing.id,
                clientIp = ip,
                scanDate = today,
                scanCount = existing.scanCount + 1,
            ))
        } else {
            scanUsageRepository.save(ScanUsageEntity(clientIp = ip, scanDate = today, scanCount = 1))
        }
    }

    private fun maxScansFor(ip: String): Int {
        val hasRegisteredEmail = scanUsageRepository.findByClientIpAndScanDate(ip, LocalDate.now()) != null
            && emailLeadRepository.existsByEmail(ip).not()
        // Check if any email is registered from this IP
        return if (emailLeadRepository.count() > 0 && emailLeadRepository.findAll().any { it.clientIp == ip }) {
            FREE_DAILY_LIMIT + EMAIL_BONUS
        } else {
            FREE_DAILY_LIMIT
        }
    }

    private fun saveResult(response: ScanResponse): ScanResultEntity {
        val entity = ScanResultEntity(
            scanType = response.scanType,
            target = response.target,
            reachable = response.reachable,
            decision = response.decision,
            totalFindings = response.summary.total,
            criticalCount = response.summary.critical,
            highCount = response.summary.high,
            mediumCount = response.summary.medium,
            lowCount = response.summary.low,
            infoCount = response.summary.info,
            categories = response.summary.categories.joinToString(","),
            findingsJson = objectMapper.writeValueAsString(response.findings),
        )
        return scanResultRepository.save(entity)
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        // Cloud Run: rightmost X-Forwarded-For entry is the actual client IP
        val xff = request.getHeader("X-Forwarded-For")
        if (xff != null) {
            val parts = xff.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) return parts[parts.size - 2] // second from right = client
            if (parts.isNotEmpty()) return parts.last()
        }
        return request.remoteAddr
    }
}

data class UrlScanRequest(@field:NotBlank val url: String)
data class GitHubScanRequest(@field:NotBlank val repoUrl: String)
data class EmailRegistrationRequest(@field:NotBlank @field:Email val email: String)

data class ScanResponse(
    val id: String? = null,
    val scanType: String,
    val target: String,
    val reachable: Boolean,
    val decision: String,
    val summary: ScanSummary,
    val findings: List<ScanFindingResponse>,
    val createdAt: String? = null,
    val remainingScans: Int? = null,
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
                    val guide = FindingGuideMap.getGuide(f.code)
                    ScanFindingResponse(
                        severity = f.severity.name,
                        category = f.category.name,
                        code = f.code,
                        title = f.title,
                        detail = f.detail,
                        evidence = f.evidence,
                        risk = guide?.risk,
                        impact = guide?.impact,
                        fixes = guide?.fixes?.map { fix ->
                            PlatformFixResponse(fix.platform, fix.instruction, fix.code)
                        },
                    )
                },
            )
        }
    }
}

data class ScanSummary(
    val total: Int, val critical: Int, val high: Int,
    val medium: Int, val low: Int, val info: Int,
    val categories: List<String>,
)

data class PlatformFixResponse(
    val platform: String,
    val instruction: String,
    val code: String? = null,
)

data class ScanFindingResponse(
    val severity: String, val category: String, val code: String,
    val title: String, val detail: String, val evidence: String,
    val risk: String? = null, val impact: String? = null,
    val fixes: List<PlatformFixResponse>? = null,
)
