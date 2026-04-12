package me.hanhyur.gatewell.assessment.domain.model

data class Finding(
    val severity: Severity,
    val category: RiskCategory,
    val code: FindingCode,
    val message: String,
) {
    companion object {
        fun high(category: RiskCategory, code: FindingCode, message: String): Finding =
            Finding(severity = Severity.HIGH, category = category, code = code, message = message)

        fun medium(category: RiskCategory, code: FindingCode, message: String): Finding =
            Finding(severity = Severity.MEDIUM, category = category, code = code, message = message)

        fun low(category: RiskCategory, code: FindingCode, message: String): Finding =
            Finding(severity = Severity.LOW, category = category, code = code, message = message)
    }
}
