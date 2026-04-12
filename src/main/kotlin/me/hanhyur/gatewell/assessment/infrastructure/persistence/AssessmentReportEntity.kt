package me.hanhyur.gatewell.assessment.infrastructure.persistence

import jakarta.persistence.*
import me.hanhyur.gatewell.assessment.domain.model.*
import me.hanhyur.gatewell.assessment.domain.policy.DecisionResult
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "assessment_reports")
class AssessmentReportEntity(
    @Id
    val id: UUID,
    val productName: String,
    @Column(length = 2000)
    val summary: String,
    @Column(length = 4000)
    val evidences: String,
    val capabilities: String,
    @Column(length = 4000)
    val findings: String,
    @Enumerated(EnumType.STRING)
    val severity: Severity,
    @Enumerated(EnumType.STRING)
    val decision: LaunchDecision,
    @Column(length = 1000)
    val recommendation: String,
    val ruleVersion: String,
    val createdAt: Instant,
) {
    companion object {
        private const val DELIMITER = "||"

        fun from(report: AssessmentReport): AssessmentReportEntity = AssessmentReportEntity(
            id = report.id.value,
            productName = report.productName.value,
            summary = report.summary,
            evidences = report.evidences.joinToString(DELIMITER) { it.value },
            capabilities = report.capabilities.joinToString(DELIMITER) { it.name },
            findings = report.findings.joinToString(DELIMITER) { "${it.severity}:${it.category}:${it.code.value}:${it.message}" },
            severity = report.severity,
            decision = report.decision,
            recommendation = report.recommendation.value,
            ruleVersion = report.ruleVersion.value,
            createdAt = report.createdAt,
        )
    }

    fun toDomain(): AssessmentReport = AssessmentReport(
        id = AssessmentId(id),
        productName = ProductName(productName),
        summary = summary,
        evidences = if (evidences.isBlank()) emptyList() else evidences.split(DELIMITER).map { Evidence(it) },
        capabilities = if (capabilities.isBlank()) emptySet() else capabilities.split(DELIMITER).map { Capability.valueOf(it) }.toSet(),
        findings = if (findings.isBlank()) emptyList() else findings.split(DELIMITER).map { parseFinding(it) },
        severity = severity,
        decision = decision,
        recommendation = Recommendation(recommendation),
        ruleVersion = RuleVersion(ruleVersion),
        createdAt = createdAt,
    )

    private fun parseFinding(encoded: String): Finding {
        val parts = encoded.split(":", limit = 4)
        return Finding(
            severity = Severity.valueOf(parts[0]),
            category = RiskCategory.valueOf(parts[1]),
            code = FindingCode(parts[2]),
            message = parts[3],
        )
    }
}
