package me.hanhyur.gatewell.assessment.application

import me.hanhyur.gatewell.assessment.domain.model.*
import me.hanhyur.gatewell.assessment.domain.policy.LaunchDecisionPolicy
import me.hanhyur.gatewell.assessment.domain.policy.RiskScoringEngine
import me.hanhyur.gatewell.assessment.domain.policy.RuleVersionInfo
import me.hanhyur.gatewell.assessment.domain.port.AssessmentReportStore
import org.springframework.stereotype.Service

@Service
class AssessmentService(
    private val riskScoringEngine: RiskScoringEngine,
    private val launchDecisionPolicy: LaunchDecisionPolicy,
    private val assessmentReportStore: AssessmentReportStore,
) {
    fun assess(command: AssessmentCommand): AssessmentReport {
        val scoringResult = riskScoringEngine.evaluate(command)
        val decisionResult = launchDecisionPolicy.decide(scoringResult)
        val report = AssessmentReport.create(command, scoringResult, decisionResult)
        return assessmentReportStore.save(report)
    }

    fun findById(id: AssessmentId): AssessmentReport? {
        return assessmentReportStore.findById(id)
    }

    fun findAll(decision: LaunchDecision?, severity: Severity?): List<AssessmentReport> {
        return assessmentReportStore.findAll()
            .let { reports -> decision?.let { d -> reports.filter { it.decision == d } } ?: reports }
            .let { reports -> severity?.let { s -> reports.filter { it.severity == s } } ?: reports }
    }

    fun reassess(originalId: AssessmentId, newEvidences: List<Evidence>): AssessmentReport? {
        val original = assessmentReportStore.findById(originalId) ?: return null
        val command = AssessmentCommand(
            productName = original.productName,
            summary = original.summary,
            evidences = newEvidences,
            capabilities = original.capabilities,
            ruleVersion = RuleVersionInfo.CURRENT,
        )
        return assess(command)
    }
}
