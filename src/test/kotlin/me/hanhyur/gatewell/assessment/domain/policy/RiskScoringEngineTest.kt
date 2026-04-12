package me.hanhyur.gatewell.assessment.domain.policy

import me.hanhyur.gatewell.assessment.application.AssessmentCommand
import me.hanhyur.gatewell.assessment.domain.model.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RiskScoringEngineTest {

    private val engine = RiskScoringEngine()

    private fun command(
        capabilities: Set<Capability> = emptySet(),
        evidences: List<Evidence> = listOf(Evidence("default evidence")),
        summary: String = "A test AI product",
    ) = AssessmentCommand(
        productName = ProductName("Test Product"),
        summary = summary,
        evidences = evidences,
        capabilities = capabilities,
        ruleVersion = RuleVersion("1.0.0"),
    )

    // --- Existing rules ---

    @Test
    fun `no capabilities produces no findings`() {
        val result = engine.evaluate(command())

        assertTrue(result.findings.isEmpty())
        assertEquals(Severity.NONE, result.severity)
    }

    @Test
    fun `CODE_EXECUTION capability produces HIGH finding in AUTH_WEAKNESS`() {
        val result = engine.evaluate(command(capabilities = setOf(Capability.CODE_EXECUTION)))

        assertEquals(1, result.findings.size)
        val finding = result.findings.first()
        assertEquals(Severity.HIGH, finding.severity)
        assertEquals(RiskCategory.AUTH_WEAKNESS, finding.category)
        assertEquals(FindingCode("CAPABILITY_CODE_EXECUTION"), finding.code)
    }

    @Test
    fun `USER_DATA_PROCESSING capability produces MEDIUM finding in DATA_LEAKAGE`() {
        val result = engine.evaluate(command(capabilities = setOf(Capability.USER_DATA_PROCESSING)))

        assertEquals(1, result.findings.size)
        val finding = result.findings.first()
        assertEquals(Severity.MEDIUM, finding.severity)
        assertEquals(RiskCategory.DATA_LEAKAGE, finding.category)
        assertEquals(FindingCode("CAPABILITY_USER_DATA_PROCESSING"), finding.code)
    }

    @Test
    fun `multiple capabilities produce multiple findings with highest severity`() {
        val result = engine.evaluate(
            command(capabilities = setOf(Capability.CODE_EXECUTION, Capability.USER_DATA_PROCESSING))
        )

        assertEquals(2, result.findings.size)
        assertEquals(Severity.HIGH, result.severity)
    }

    @Test
    fun `EXTERNAL_API_CALL capability produces MEDIUM finding in RATE_LIMITING`() {
        val result = engine.evaluate(command(capabilities = setOf(Capability.EXTERNAL_API_CALL)))

        assertEquals(1, result.findings.size)
        val finding = result.findings.first()
        assertEquals(Severity.MEDIUM, finding.severity)
        assertEquals(RiskCategory.RATE_LIMITING, finding.category)
    }

    @Test
    fun `evaluation is deterministic - same input produces same output`() {
        val cmd = command(capabilities = setOf(Capability.CODE_EXECUTION, Capability.USER_DATA_PROCESSING))

        val result1 = engine.evaluate(cmd)
        val result2 = engine.evaluate(cmd)

        assertEquals(result1, result2)
    }

    // --- New capability rules ---

    @Test
    fun `WEB_BROWSING capability produces HIGH finding in PROMPT_INJECTION`() {
        val result = engine.evaluate(command(capabilities = setOf(Capability.WEB_BROWSING)))

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_WEB_BROWSING") }
        assertEquals(Severity.HIGH, finding?.severity)
        assertEquals(RiskCategory.PROMPT_INJECTION, finding?.category)
    }

    @Test
    fun `FILE_ACCESS capability produces MEDIUM finding in DATA_LEAKAGE`() {
        val result = engine.evaluate(command(capabilities = setOf(Capability.FILE_ACCESS)))

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_FILE_ACCESS") }
        assertEquals(Severity.MEDIUM, finding?.severity)
        assertEquals(RiskCategory.DATA_LEAKAGE, finding?.category)
    }

    @Test
    fun `DATABASE_ACCESS capability produces HIGH finding in DATA_LEAKAGE`() {
        val result = engine.evaluate(command(capabilities = setOf(Capability.DATABASE_ACCESS)))

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_DATABASE_ACCESS") }
        assertEquals(Severity.HIGH, finding?.severity)
        assertEquals(RiskCategory.DATA_LEAKAGE, finding?.category)
    }

    // --- Vibe coding full-stack scenario ---

    @Test
    fun `vibe-coded app with all capabilities produces multiple HIGH findings including combos`() {
        val result = engine.evaluate(
            command(capabilities = Capability.entries.toSet())
        )

        // 6 capability + 3 combo rules
        assertEquals(9, result.findings.size)
        assertEquals(Severity.HIGH, result.severity)
        assertTrue(result.findings.count { it.severity == Severity.HIGH } >= 5)
    }

    // --- Evidence-based mitigation rules ---

    @Test
    fun `EXTERNAL_API_CALL with rate limiting evidence downgrades to LOW`() {
        val result = engine.evaluate(
            command(
                capabilities = setOf(Capability.EXTERNAL_API_CALL),
                evidences = listOf(Evidence("Rate limiting configured at 100 req/min")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_EXTERNAL_API_CALL") }
        assertEquals(Severity.LOW, finding?.severity)
    }

    @Test
    fun `USER_DATA_PROCESSING with encryption evidence downgrades to LOW`() {
        val result = engine.evaluate(
            command(
                capabilities = setOf(Capability.USER_DATA_PROCESSING),
                evidences = listOf(Evidence("All user data encrypted at rest with AES-256")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_USER_DATA_PROCESSING") }
        assertEquals(Severity.LOW, finding?.severity)
    }

    @Test
    fun `CODE_EXECUTION with sandbox evidence downgrades to MEDIUM`() {
        val result = engine.evaluate(
            command(
                capabilities = setOf(Capability.CODE_EXECUTION),
                evidences = listOf(Evidence("Code execution runs in sandboxed Docker container")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_CODE_EXECUTION") }
        assertEquals(Severity.MEDIUM, finding?.severity)
    }

    @Test
    fun `WEB_BROWSING with input sanitization evidence downgrades to MEDIUM`() {
        val result = engine.evaluate(
            command(
                capabilities = setOf(Capability.WEB_BROWSING),
                evidences = listOf(Evidence("Input sanitization applied via DOMPurify")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_WEB_BROWSING") }
        assertEquals(Severity.MEDIUM, finding?.severity)
    }

    @Test
    fun `unrelated evidence does not downgrade risk`() {
        val result = engine.evaluate(
            command(
                capabilities = setOf(Capability.CODE_EXECUTION),
                evidences = listOf(Evidence("We have a nice logo")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("CAPABILITY_CODE_EXECUTION") }
        assertEquals(Severity.HIGH, finding?.severity)
    }

    // --- Summary-based risk rules ---

    @Test
    fun `summary mentioning unfiltered content generation flags HARMFUL_OUTPUT`() {
        val result = engine.evaluate(
            command(summary = "AI generates unfiltered text responses to any user prompt")
        )

        val finding = result.findings.find { it.code == FindingCode("SUMMARY_HARMFUL_OUTPUT") }
        assertEquals(Severity.HIGH, finding?.severity)
        assertEquals(RiskCategory.HARMFUL_OUTPUT, finding?.category)
    }

    @Test
    fun `summary mentioning public-facing without auth flags ABUSE_SPAM`() {
        val result = engine.evaluate(
            command(summary = "Public-facing chatbot with no authentication required")
        )

        val finding = result.findings.find { it.code == FindingCode("SUMMARY_ABUSE_SPAM") }
        assertEquals(Severity.MEDIUM, finding?.severity)
        assertEquals(RiskCategory.ABUSE_SPAM, finding?.category)
    }

    @Test
    fun `summary mentioning pay-per-call or token billing flags COST_EXPLOSION`() {
        val result = engine.evaluate(
            command(summary = "App uses pay-per-call GPT-4 API for every user request")
        )

        val finding = result.findings.find { it.code == FindingCode("SUMMARY_COST_EXPLOSION") }
        assertEquals(Severity.MEDIUM, finding?.severity)
        assertEquals(RiskCategory.COST_EXPLOSION, finding?.category)
    }

    @Test
    fun `summary with no monitoring keywords flags OBSERVABILITY_GAPS`() {
        val result = engine.evaluate(
            command(
                summary = "Simple chatbot deployed to production",
                evidences = listOf(Evidence("No monitoring configured")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("SUMMARY_OBSERVABILITY_GAPS") }
        assertEquals(Severity.LOW, finding?.severity)
        assertEquals(RiskCategory.OBSERVABILITY_GAPS, finding?.category)
    }

    @Test
    fun `observability finding suppressed when logging evidence exists`() {
        val result = engine.evaluate(
            command(
                summary = "Simple chatbot deployed to production",
                evidences = listOf(Evidence("Structured logging with Datadog monitoring")),
            )
        )

        val finding = result.findings.find { it.code == FindingCode("SUMMARY_OBSERVABILITY_GAPS") }
        assertEquals(null, finding)
    }

    @Test
    fun `summary mentioning no fallback flags FALLBACK_FAILURES`() {
        val result = engine.evaluate(
            command(summary = "AI agent with no fallback when model API is unavailable")
        )

        val finding = result.findings.find { it.code == FindingCode("SUMMARY_FALLBACK_FAILURES") }
        assertEquals(Severity.MEDIUM, finding?.severity)
        assertEquals(RiskCategory.FALLBACK_FAILURES, finding?.category)
    }

    @Test
    fun `normal summary without risk signals produces no summary findings`() {
        val result = engine.evaluate(
            command(summary = "Internal tool for team collaboration with Slack integration")
        )

        val summaryFindings = result.findings.filter { it.code.value.startsWith("SUMMARY_") }
        assertTrue(summaryFindings.isEmpty())
    }

    // --- Combined capability + summary rules ---

    @Test
    fun `combined capability and summary risks accumulate`() {
        val result = engine.evaluate(
            command(
                capabilities = setOf(Capability.CODE_EXECUTION),
                summary = "Public-facing unfiltered AI with pay-per-call billing and no fallback",
            )
        )

        assertTrue(result.findings.size >= 4)
        assertEquals(Severity.HIGH, result.severity)
    }

    // --- Combination risk rules ---

    @Test
    fun `CODE_EXECUTION + WEB_BROWSING combo produces additional HIGH finding`() {
        val result = engine.evaluate(
            command(capabilities = setOf(Capability.CODE_EXECUTION, Capability.WEB_BROWSING))
        )

        val combo = result.findings.find { it.code == FindingCode("COMBO_CODE_EXEC_WEB_BROWSE") }
        assertNotNull(combo)
        assertEquals(Severity.HIGH, combo?.severity)
        assertEquals(RiskCategory.PROMPT_INJECTION, combo?.category)
    }

    @Test
    fun `DATABASE_ACCESS + USER_DATA_PROCESSING combo produces additional HIGH finding`() {
        val result = engine.evaluate(
            command(capabilities = setOf(Capability.DATABASE_ACCESS, Capability.USER_DATA_PROCESSING))
        )

        val combo = result.findings.find { it.code == FindingCode("COMBO_DB_USER_DATA") }
        assertNotNull(combo)
        assertEquals(Severity.HIGH, combo?.severity)
        assertEquals(RiskCategory.DATA_LEAKAGE, combo?.category)
    }

    @Test
    fun `3+ unmitigated capabilities produces breadth warning`() {
        val result = engine.evaluate(
            command(capabilities = setOf(
                Capability.CODE_EXECUTION,
                Capability.FILE_ACCESS,
                Capability.EXTERNAL_API_CALL,
            ))
        )

        val breadth = result.findings.find { it.code == FindingCode("COMBO_BROAD_CAPABILITY_SURFACE") }
        assertNotNull(breadth)
        assertEquals(Severity.MEDIUM, breadth?.severity)
    }

    @Test
    fun `2 capabilities does not trigger breadth warning`() {
        val result = engine.evaluate(
            command(capabilities = setOf(Capability.FILE_ACCESS, Capability.EXTERNAL_API_CALL))
        )

        val breadth = result.findings.find { it.code == FindingCode("COMBO_BROAD_CAPABILITY_SURFACE") }
        assertEquals(null, breadth)
    }

    @Test
    fun `combo rule not triggered when only one of the pair exists`() {
        val result = engine.evaluate(
            command(capabilities = setOf(Capability.CODE_EXECUTION))
        )

        val combo = result.findings.find { it.code == FindingCode("COMBO_CODE_EXEC_WEB_BROWSE") }
        assertEquals(null, combo)
    }
}
