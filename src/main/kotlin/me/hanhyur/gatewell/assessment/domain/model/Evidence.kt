package me.hanhyur.gatewell.assessment.domain.model

@JvmInline
value class Evidence(val value: String) {
    init {
        require(value.isNotBlank()) { "evidence must not be blank" }
    }
}
