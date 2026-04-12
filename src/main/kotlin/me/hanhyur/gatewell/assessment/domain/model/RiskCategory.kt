package me.hanhyur.gatewell.assessment.domain.model

enum class RiskCategory {
    PROMPT_INJECTION,
    DATA_LEAKAGE,
    HARMFUL_OUTPUT,
    AUTH_WEAKNESS,
    ABUSE_SPAM,
    RATE_LIMITING,
    COST_EXPLOSION,
    OBSERVABILITY_GAPS,
    FALLBACK_FAILURES,
}
