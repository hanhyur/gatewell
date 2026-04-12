package me.hanhyur.gatewell.assessment.api

import me.hanhyur.gatewell.assessment.domain.model.AssessmentReport

data class CompareResponse(
    val before: CompareSnapshot,
    val after: CompareSnapshot,
    val decisionChanged: Boolean,
    val severityChanged: Boolean,
    val resolvedFindings: List<String>,
    val newFindings: List<String>,
) {
    companion object {
        fun from(before: AssessmentReport, after: AssessmentReport): CompareResponse {
            val beforeCodes = before.findings.map { it.code.value }.toSet()
            val afterCodes = after.findings.map { it.code.value }.toSet()

            return CompareResponse(
                before = CompareSnapshot(
                    id = before.id.value.toString(),
                    severity = before.severity.name,
                    launchDecision = before.decision.name,
                    findingsCount = before.findings.size,
                ),
                after = CompareSnapshot(
                    id = after.id.value.toString(),
                    severity = after.severity.name,
                    launchDecision = after.decision.name,
                    findingsCount = after.findings.size,
                ),
                decisionChanged = before.decision != after.decision,
                severityChanged = before.severity != after.severity,
                resolvedFindings = (beforeCodes - afterCodes).toList().sorted(),
                newFindings = (afterCodes - beforeCodes).toList().sorted(),
            )
        }
    }
}

data class CompareSnapshot(
    val id: String,
    val severity: String,
    val launchDecision: String,
    val findingsCount: Int,
)
