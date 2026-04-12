package me.hanhyur.gatewell.assessment.domain.policy

import me.hanhyur.gatewell.assessment.domain.model.*
import org.springframework.stereotype.Component

@Component
class LaunchDecisionPolicy {

    fun decide(scoringResult: ScoringResult): DecisionResult {
        val decision = when (scoringResult.severity) {
            Severity.HIGH -> LaunchDecision.BLOCK
            Severity.MEDIUM -> LaunchDecision.CAUTION
            Severity.LOW, Severity.NONE -> LaunchDecision.ALLOW
        }

        val recommendation = when (decision) {
            LaunchDecision.BLOCK -> Recommendation(
                "Launch blocked due to high-severity risks. Resolve all HIGH findings before proceeding."
            )
            LaunchDecision.CAUTION -> Recommendation(
                "Launch may proceed with caution. Address medium-severity findings to reduce risk."
            )
            LaunchDecision.ALLOW -> Recommendation(
                "No significant risks detected. Product is cleared for launch."
            )
        }

        return DecisionResult(decision = decision, recommendation = recommendation)
    }
}
