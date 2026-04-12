package me.hanhyur.gatewell.common.api

data class ApiErrorResponse(
    val error: String,
    val details: List<String> = emptyList(),
)
