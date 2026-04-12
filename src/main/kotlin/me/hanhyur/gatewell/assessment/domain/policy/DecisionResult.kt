package me.hanhyur.gatewell.assessment.domain.policy

import me.hanhyur.gatewell.assessment.domain.model.LaunchDecision
import me.hanhyur.gatewell.assessment.domain.model.Recommendation

data class DecisionResult(
    val decision: LaunchDecision,
    val recommendation: Recommendation,
)
