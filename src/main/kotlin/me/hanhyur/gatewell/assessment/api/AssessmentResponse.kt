package me.hanhyur.gatewell.assessment.api

import me.hanhyur.gatewell.assessment.domain.model.AssessmentReport

data class AssessmentResponse(
    val id: String,
    val productName: String,
    val summary: String,
    val evidences: List<String>,
    val capabilities: List<String>,
    val findings: List<FindingResponse>,
    val findingsSummary: FindingsSummary,
    val severity: String,
    val launchDecision: String,
    val recommendation: String,
    val ruleVersion: String,
    val createdAt: String,
) {
    companion object {
        fun from(report: AssessmentReport): AssessmentResponse {
            val findingResponses = report.findings.map {
                FindingResponse(it.severity.name, it.category.name, it.code.value, it.message)
            }
            return AssessmentResponse(
                id = report.id.value.toString(),
                productName = report.productName.value,
                summary = report.summary,
                evidences = report.evidences.map { it.value },
                capabilities = report.capabilities.map { it.name },
                findings = findingResponses,
                findingsSummary = FindingsSummary.from(report),
                severity = report.severity.name,
                launchDecision = report.decision.name,
                recommendation = report.recommendation.value,
                ruleVersion = report.ruleVersion.value,
                createdAt = report.createdAt.toString(),
            )
        }
    }
}

data class FindingResponse(
    val severity: String,
    val category: String,
    val code: String,
    val message: String,
)

data class FindingsSummary(
    val total: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val categories: List<String>,
) {
    companion object {
        fun from(report: AssessmentReport): FindingsSummary {
            val findings = report.findings
            return FindingsSummary(
                total = findings.size,
                high = findings.count { it.severity.name == "HIGH" },
                medium = findings.count { it.severity.name == "MEDIUM" },
                low = findings.count { it.severity.name == "LOW" },
                categories = findings.map { it.category.name }.distinct().sorted(),
            )
        }
    }
}
