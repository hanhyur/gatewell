package me.hanhyur.gatewell.harness.domain

import me.hanhyur.gatewell.assessment.domain.model.*

data class HarnessExpectation(
    val scenarioId: ScenarioId,
    val decision: LaunchDecision,
    val severity: Severity,
    val expectedCategories: Set<RiskCategory>,
    val expectedFindingCodes: Set<FindingCode>,
)

data class HarnessActual(
    val decision: LaunchDecision,
    val severity: Severity,
    val categories: Set<RiskCategory>,
    val findingCodes: Set<FindingCode>,
)

data class HarnessResult(
    val scenarioId: ScenarioId,
    val expectedDecision: LaunchDecision,
    val actualDecision: LaunchDecision,
    val passed: Boolean,
    val mismatches: List<String>,
)
