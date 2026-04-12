package me.hanhyur.gatewell.assessment.domain.model

data class ScoringResult(
    val findings: List<Finding>,
    val severity: Severity,
)
