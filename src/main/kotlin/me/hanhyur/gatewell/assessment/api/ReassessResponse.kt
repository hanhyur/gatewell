package me.hanhyur.gatewell.assessment.api

import me.hanhyur.gatewell.assessment.domain.model.AssessmentReport

data class ReassessResponse(
    val previousAssessmentId: String,
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
        fun from(previousId: String, report: AssessmentReport): ReassessResponse {
            val base = AssessmentResponse.from(report)
            return ReassessResponse(
                previousAssessmentId = previousId,
                id = base.id,
                productName = base.productName,
                summary = base.summary,
                evidences = base.evidences,
                capabilities = base.capabilities,
                findings = base.findings,
                findingsSummary = base.findingsSummary,
                severity = base.severity,
                launchDecision = base.launchDecision,
                recommendation = base.recommendation,
                ruleVersion = base.ruleVersion,
                createdAt = base.createdAt,
            )
        }
    }
}
