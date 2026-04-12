package me.hanhyur.gatewell.assessment.domain.model

@JvmInline
value class FindingCode(val value: String) {
    init {
        require(value.isNotBlank()) { "findingCode must not be blank" }
    }
}
