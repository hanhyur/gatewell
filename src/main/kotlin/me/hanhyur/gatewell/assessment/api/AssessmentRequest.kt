package me.hanhyur.gatewell.assessment.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import me.hanhyur.gatewell.assessment.application.AssessmentCommand
import me.hanhyur.gatewell.assessment.domain.model.*
import me.hanhyur.gatewell.assessment.domain.policy.RuleVersionInfo

data class AssessmentRequest(
    @field:NotBlank
    val productName: String,
    @field:NotBlank
    val summary: String,
    @field:NotEmpty
    val evidences: List<String>,
    val capabilities: List<String> = emptyList(),
) {
    fun toCommand(): AssessmentCommand = AssessmentCommand(
        productName = ProductName(productName),
        summary = summary,
        evidences = evidences.map { Evidence(it) },
        capabilities = capabilities.map { Capability.valueOf(it) }.toSet(),
        ruleVersion = RuleVersionInfo.CURRENT,
    )
}
