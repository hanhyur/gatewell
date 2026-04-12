package me.hanhyur.gatewell.assessment.domain.policy

import me.hanhyur.gatewell.assessment.domain.model.RuleVersion

object RuleVersionInfo {
    val CURRENT = RuleVersion("1.0.0")

    val RULE_DESCRIPTIONS: List<String> = listOf(
        "CAPABILITY_CODE_EXECUTION: Code execution → AUTH_WEAKNESS (HIGH, mitigable)",
        "CAPABILITY_WEB_BROWSING: Web browsing → PROMPT_INJECTION (HIGH, mitigable)",
        "CAPABILITY_FILE_ACCESS: File access → DATA_LEAKAGE (MEDIUM, mitigable)",
        "CAPABILITY_DATABASE_ACCESS: Database access → DATA_LEAKAGE (HIGH, mitigable)",
        "CAPABILITY_EXTERNAL_API_CALL: External API calls → RATE_LIMITING (MEDIUM, mitigable)",
        "CAPABILITY_USER_DATA_PROCESSING: User data processing → DATA_LEAKAGE (MEDIUM, mitigable)",
        "SUMMARY_HARMFUL_OUTPUT: Unfiltered content generation → HARMFUL_OUTPUT (HIGH)",
        "SUMMARY_ABUSE_SPAM: Public-facing without auth → ABUSE_SPAM (MEDIUM)",
        "SUMMARY_COST_EXPLOSION: Pay-per-call billing → COST_EXPLOSION (MEDIUM)",
        "SUMMARY_OBSERVABILITY_GAPS: Production without monitoring → OBSERVABILITY_GAPS (LOW)",
        "SUMMARY_FALLBACK_FAILURES: No fallback mechanism → FALLBACK_FAILURES (MEDIUM)",
    )
}
