package me.hanhyur.gatewell.assessment.domain.model

@JvmInline
value class ProductName(val value: String) {
    init {
        require(value.isNotBlank()) { "productName must not be blank" }
    }
}
