package me.hanhyur.gatewell.assessment.domain.model

enum class Severity {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun from(findings: List<Finding>): Severity {
            if (findings.isEmpty()) return NONE
            return findings.maxOf { it.severity }
        }
    }
}
