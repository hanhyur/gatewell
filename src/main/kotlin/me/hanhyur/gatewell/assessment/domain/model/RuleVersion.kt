package me.hanhyur.gatewell.assessment.domain.model

@JvmInline
value class RuleVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "ruleVersion must not be blank" }
    }
}
