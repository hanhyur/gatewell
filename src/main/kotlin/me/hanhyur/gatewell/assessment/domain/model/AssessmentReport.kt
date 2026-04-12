package me.hanhyur.gatewell.assessment.domain.model

import me.hanhyur.gatewell.assessment.application.AssessmentCommand
import me.hanhyur.gatewell.assessment.domain.policy.DecisionResult
import java.time.Instant

data class AssessmentReport(
    val id: AssessmentId,
    val productName: ProductName,
    val summary: String,
    val evidences: List<Evidence>,
    val capabilities: Set<Capability>,
    val findings: List<Finding>,
    val severity: Severity,
    val decision: LaunchDecision,
    val recommendation: Recommendation,
    val ruleVersion: RuleVersion,
    val createdAt: Instant,
) {
    companion object {
        fun create(
            command: AssessmentCommand,
            scoringResult: ScoringResult,
            decisionResult: DecisionResult,
        ): AssessmentReport = AssessmentReport(
            id = AssessmentId.generate(),
            productName = command.productName,
            summary = command.summary,
            evidences = command.evidences,
            capabilities = command.capabilities,
            findings = scoringResult.findings,
            severity = scoringResult.severity,
            decision = decisionResult.decision,
            recommendation = decisionResult.recommendation,
            ruleVersion = command.ruleVersion,
            createdAt = Instant.now(),
        )
    }
}
