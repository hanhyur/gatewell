package me.hanhyur.gatewell.harness.domain

@JvmInline
value class ScenarioId(val value: String) {
    init {
        require(value.isNotBlank()) { "scenarioId must not be blank" }
    }
}
