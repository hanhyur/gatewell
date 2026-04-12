package me.hanhyur.gatewell.assessment.api

import jakarta.validation.constraints.NotEmpty

data class ReassessRequest(
    @field:NotEmpty
    val evidences: List<String>,
)
