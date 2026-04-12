package me.hanhyur.gatewell.assessment.application

import me.hanhyur.gatewell.assessment.domain.model.*

data class AssessmentCommand(
    val productName: ProductName,
    val summary: String,
    val evidences: List<Evidence>,
    val capabilities: Set<Capability>,
    val ruleVersion: RuleVersion,
)
