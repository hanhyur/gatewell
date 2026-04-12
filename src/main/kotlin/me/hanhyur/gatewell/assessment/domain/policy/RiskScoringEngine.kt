package me.hanhyur.gatewell.assessment.domain.policy

import me.hanhyur.gatewell.assessment.application.AssessmentCommand
import me.hanhyur.gatewell.assessment.domain.model.*
import org.springframework.stereotype.Component

@Component
class RiskScoringEngine {

    fun evaluate(command: AssessmentCommand): ScoringResult {
        val findings = mutableListOf<Finding>()
        val evidenceText = command.evidences.joinToString(" ") { it.value }.lowercase()

        if (Capability.CODE_EXECUTION in command.capabilities) {
            val mitigated = evidenceText.containsAny("sandbox", "isolated", "container")
            findings += Finding(
                severity = if (mitigated) Severity.MEDIUM else Severity.HIGH,
                category = RiskCategory.AUTH_WEAKNESS,
                code = FindingCode("CAPABILITY_CODE_EXECUTION"),
                message = if (mitigated)
                    "Code execution detected with isolation evidence. Risk reduced but not eliminated."
                else
                    "Code execution raises launch risk without strong isolation evidence.",
            )
        }

        if (Capability.WEB_BROWSING in command.capabilities) {
            val mitigated = evidenceText.containsAny("sanitiz", "purify", "escape", "csp")
            findings += Finding(
                severity = if (mitigated) Severity.MEDIUM else Severity.HIGH,
                category = RiskCategory.PROMPT_INJECTION,
                code = FindingCode("CAPABILITY_WEB_BROWSING"),
                message = if (mitigated)
                    "Web browsing with input sanitization evidence. Prompt injection risk reduced."
                else
                    "Web browsing exposes the system to prompt injection via external content.",
            )
        }

        if (Capability.FILE_ACCESS in command.capabilities) {
            val mitigated = evidenceText.containsAny("read-only", "readonly", "allowlist", "whitelist")
            findings += Finding(
                severity = if (mitigated) Severity.LOW else Severity.MEDIUM,
                category = RiskCategory.DATA_LEAKAGE,
                code = FindingCode("CAPABILITY_FILE_ACCESS"),
                message = if (mitigated)
                    "File access restricted with access controls. Residual leakage risk is low."
                else
                    "File access without explicit restrictions increases data leakage risk.",
            )
        }

        if (Capability.DATABASE_ACCESS in command.capabilities) {
            val mitigated = evidenceText.containsAny("parameterized", "prepared statement", "orm", "read-only")
            findings += Finding(
                severity = if (mitigated) Severity.MEDIUM else Severity.HIGH,
                category = RiskCategory.DATA_LEAKAGE,
                code = FindingCode("CAPABILITY_DATABASE_ACCESS"),
                message = if (mitigated)
                    "Database access with query safety evidence. Risk partially mitigated."
                else
                    "Direct database access without query safety raises data leakage and injection risk.",
            )
        }

        if (Capability.EXTERNAL_API_CALL in command.capabilities) {
            val mitigated = evidenceText.containsAny("rate limit", "throttl", "circuit breaker")
            findings += Finding(
                severity = if (mitigated) Severity.LOW else Severity.MEDIUM,
                category = RiskCategory.RATE_LIMITING,
                code = FindingCode("CAPABILITY_EXTERNAL_API_CALL"),
                message = if (mitigated)
                    "External API calls with rate limiting evidence. Cost explosion risk reduced."
                else
                    "External API calls require rate limiting to prevent cost explosion.",
            )
        }

        if (Capability.USER_DATA_PROCESSING in command.capabilities) {
            val mitigated = evidenceText.containsAny("encrypt", "anonymiz", "pseudonymiz", "hashing")
            findings += Finding(
                severity = if (mitigated) Severity.LOW else Severity.MEDIUM,
                category = RiskCategory.DATA_LEAKAGE,
                code = FindingCode("CAPABILITY_USER_DATA_PROCESSING"),
                message = if (mitigated)
                    "User data processing with encryption/anonymization evidence. Leakage risk reduced."
                else
                    "Processing user data increases data leakage risk.",
            )
        }

        evaluateSummaryRules(command.summary.lowercase(), evidenceText, findings)
        evaluateComboRules(command.capabilities, findings)

        val severity = Severity.from(findings)
        return ScoringResult(findings = findings.toList(), severity = severity)
    }

    private fun evaluateComboRules(capabilities: Set<Capability>, findings: MutableList<Finding>) {
        if (Capability.CODE_EXECUTION in capabilities && Capability.WEB_BROWSING in capabilities) {
            findings += Finding.high(
                category = RiskCategory.PROMPT_INJECTION,
                code = FindingCode("COMBO_CODE_EXEC_WEB_BROWSE"),
                message = "Code execution combined with web browsing creates a high-risk attack vector for prompt injection leading to arbitrary code execution.",
            )
        }

        if (Capability.DATABASE_ACCESS in capabilities && Capability.USER_DATA_PROCESSING in capabilities) {
            findings += Finding.high(
                category = RiskCategory.DATA_LEAKAGE,
                code = FindingCode("COMBO_DB_USER_DATA"),
                message = "Direct database access combined with user data processing significantly increases data exfiltration risk.",
            )
        }

        if (capabilities.size >= 3) {
            findings += Finding.medium(
                category = RiskCategory.AUTH_WEAKNESS,
                code = FindingCode("COMBO_BROAD_CAPABILITY_SURFACE"),
                message = "Broad capability surface (${capabilities.size} capabilities) increases overall attack surface.",
            )
        }
    }

    private fun evaluateSummaryRules(summary: String, evidenceText: String, findings: MutableList<Finding>) {
        if (summary.containsAny("unfiltered", "uncensored", "no content filter", "unrestricted output")) {
            findings += Finding.high(
                category = RiskCategory.HARMFUL_OUTPUT,
                code = FindingCode("SUMMARY_HARMFUL_OUTPUT"),
                message = "Product description suggests unfiltered content generation, risking harmful output.",
            )
        }

        if (summary.containsAny("public-facing", "public facing", "no auth", "without auth", "open access")) {
            findings += Finding.medium(
                category = RiskCategory.ABUSE_SPAM,
                code = FindingCode("SUMMARY_ABUSE_SPAM"),
                message = "Public-facing service without authentication is vulnerable to abuse and spam.",
            )
        }

        if (summary.containsAny("pay-per-call", "pay per call", "token billing", "per-request cost", "per request cost")) {
            findings += Finding.medium(
                category = RiskCategory.COST_EXPLOSION,
                code = FindingCode("SUMMARY_COST_EXPLOSION"),
                message = "Usage-based billing model without spending controls risks cost explosion.",
            )
        }

        val hasObservability = evidenceText.containsAny("monitor", "logging", "observ", "tracing", "datadog", "grafana", "prometheus")
            && !evidenceText.containsAny("no monitor", "no logging", "no observ", "no tracing")
        if (!hasObservability && summary.containsAny("production", "deploy", "live", "launch")) {
            findings += Finding.low(
                category = RiskCategory.OBSERVABILITY_GAPS,
                code = FindingCode("SUMMARY_OBSERVABILITY_GAPS"),
                message = "Production deployment without evidence of monitoring or logging.",
            )
        }

        if (summary.containsAny("no fallback", "no retry", "no graceful", "single point of failure")) {
            findings += Finding.medium(
                category = RiskCategory.FALLBACK_FAILURES,
                code = FindingCode("SUMMARY_FALLBACK_FAILURES"),
                message = "No fallback mechanism described for service unavailability.",
            )
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
