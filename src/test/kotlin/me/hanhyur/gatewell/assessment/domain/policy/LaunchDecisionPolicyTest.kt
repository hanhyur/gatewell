package me.hanhyur.gatewell.assessment.domain.policy

import me.hanhyur.gatewell.assessment.application.AssessmentCommand
import me.hanhyur.gatewell.assessment.domain.model.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LaunchDecisionPolicyTest {

    private val policy = LaunchDecisionPolicy()

    @Test
    fun `HIGH severity results in BLOCK decision`() {
        val scoringResult = ScoringResult(
            findings = listOf(
                Finding.high(
                    category = RiskCategory.AUTH_WEAKNESS,
                    code = FindingCode("TEST_HIGH"),
                    message = "High risk finding",
                )
            ),
            severity = Severity.HIGH,
        )

        val result = policy.decide(scoringResult)

        assertEquals(LaunchDecision.BLOCK, result.decision)
    }

    @Test
    fun `MEDIUM severity results in CAUTION decision`() {
        val scoringResult = ScoringResult(
            findings = listOf(
                Finding.medium(
                    category = RiskCategory.RATE_LIMITING,
                    code = FindingCode("TEST_MEDIUM"),
                    message = "Medium risk finding",
                )
            ),
            severity = Severity.MEDIUM,
        )

        val result = policy.decide(scoringResult)

        assertEquals(LaunchDecision.CAUTION, result.decision)
    }

    @Test
    fun `LOW severity results in ALLOW decision`() {
        val scoringResult = ScoringResult(
            findings = listOf(
                Finding.low(
                    category = RiskCategory.OBSERVABILITY_GAPS,
                    code = FindingCode("TEST_LOW"),
                    message = "Low risk finding",
                )
            ),
            severity = Severity.LOW,
        )

        val result = policy.decide(scoringResult)

        assertEquals(LaunchDecision.ALLOW, result.decision)
    }

    @Test
    fun `NONE severity results in ALLOW decision`() {
        val scoringResult = ScoringResult(
            findings = emptyList(),
            severity = Severity.NONE,
        )

        val result = policy.decide(scoringResult)

        assertEquals(LaunchDecision.ALLOW, result.decision)
    }

    @Test
    fun `decision includes recommendation`() {
        val scoringResult = ScoringResult(
            findings = listOf(
                Finding.high(
                    category = RiskCategory.PROMPT_INJECTION,
                    code = FindingCode("PI_001"),
                    message = "Prompt injection detected",
                )
            ),
            severity = Severity.HIGH,
        )

        val result = policy.decide(scoringResult)

        assertEquals(LaunchDecision.BLOCK, result.decision)
        assertTrue(result.recommendation.value.isNotBlank())
    }

    @Test
    fun `decision is deterministic`() {
        val scoringResult = ScoringResult(
            findings = listOf(
                Finding.high(
                    category = RiskCategory.AUTH_WEAKNESS,
                    code = FindingCode("AUTH_001"),
                    message = "Auth weakness",
                )
            ),
            severity = Severity.HIGH,
        )

        val result1 = policy.decide(scoringResult)
        val result2 = policy.decide(scoringResult)

        assertEquals(result1, result2)
    }
}

class AssessmentReportTest {

    @Test
    fun `create assembles report from command, scoring, and decision`() {
        val command = AssessmentCommand(
            productName = ProductName("Test Product"),
            summary = "Test summary",
            evidences = listOf(Evidence("evidence 1")),
            capabilities = setOf(Capability.CODE_EXECUTION),
            ruleVersion = RuleVersion("1.0.0"),
        )
        val scoringResult = ScoringResult(
            findings = listOf(
                Finding.high(
                    category = RiskCategory.AUTH_WEAKNESS,
                    code = FindingCode("AUTH_001"),
                    message = "Auth weakness",
                )
            ),
            severity = Severity.HIGH,
        )
        val decisionResult = DecisionResult(
            decision = LaunchDecision.BLOCK,
            recommendation = Recommendation("Do not launch without isolation."),
        )

        val report = AssessmentReport.create(command, scoringResult, decisionResult)

        assertEquals(ProductName("Test Product"), report.productName)
        assertEquals(Severity.HIGH, report.severity)
        assertEquals(LaunchDecision.BLOCK, report.decision)
        assertEquals(1, report.findings.size)
        assertEquals(RuleVersion("1.0.0"), report.ruleVersion)
    }
}
