package me.hanhyur.gatewell.assessment.domain.model

@JvmInline
value class Recommendation(val value: String) {
    init {
        require(value.isNotBlank()) { "recommendation must not be blank" }
    }
}
