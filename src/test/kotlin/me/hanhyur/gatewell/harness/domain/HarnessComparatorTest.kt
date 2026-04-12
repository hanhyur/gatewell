package me.hanhyur.gatewell.harness.domain

import me.hanhyur.gatewell.assessment.domain.model.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HarnessComparatorTest {

    private val comparator = HarnessComparator()

    @Test
    fun `passes when all fields match`() {
        val expected = HarnessExpectation(
            scenarioId = ScenarioId("prompt-injection-basic"),
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            expectedCategories = setOf(RiskCategory.AUTH_WEAKNESS),
            expectedFindingCodes = setOf(FindingCode("CAPABILITY_CODE_EXECUTION")),
        )

        val actual = HarnessActual(
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            categories = setOf(RiskCategory.AUTH_WEAKNESS),
            findingCodes = setOf(FindingCode("CAPABILITY_CODE_EXECUTION")),
        )

        val result = comparator.compare(expected, actual)

        assertTrue(result.passed)
        assertTrue(result.mismatches.isEmpty())
        assertEquals(ScenarioId("prompt-injection-basic"), result.scenarioId)
        assertEquals(LaunchDecision.BLOCK, result.expectedDecision)
        assertEquals(LaunchDecision.BLOCK, result.actualDecision)
    }

    @Test
    fun `fails when decision mismatches`() {
        val expected = HarnessExpectation(
            scenarioId = ScenarioId("test-scenario"),
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            expectedCategories = emptySet(),
            expectedFindingCodes = emptySet(),
        )

        val actual = HarnessActual(
            decision = LaunchDecision.ALLOW,
            severity = Severity.HIGH,
            categories = emptySet(),
            findingCodes = emptySet(),
        )

        val result = comparator.compare(expected, actual)

        assertFalse(result.passed)
        assertTrue(result.mismatches.any { "decision" in it.lowercase() })
    }

    @Test
    fun `fails when severity mismatches`() {
        val expected = HarnessExpectation(
            scenarioId = ScenarioId("test-scenario"),
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            expectedCategories = emptySet(),
            expectedFindingCodes = emptySet(),
        )

        val actual = HarnessActual(
            decision = LaunchDecision.BLOCK,
            severity = Severity.MEDIUM,
            categories = emptySet(),
            findingCodes = emptySet(),
        )

        val result = comparator.compare(expected, actual)

        assertFalse(result.passed)
        assertTrue(result.mismatches.any { "severity" in it.lowercase() })
    }

    @Test
    fun `fails when expected category is missing`() {
        val expected = HarnessExpectation(
            scenarioId = ScenarioId("test-scenario"),
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            expectedCategories = setOf(RiskCategory.AUTH_WEAKNESS, RiskCategory.DATA_LEAKAGE),
            expectedFindingCodes = emptySet(),
        )

        val actual = HarnessActual(
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            categories = setOf(RiskCategory.AUTH_WEAKNESS),
            findingCodes = emptySet(),
        )

        val result = comparator.compare(expected, actual)

        assertFalse(result.passed)
        assertTrue(result.mismatches.any { "categor" in it.lowercase() })
    }

    @Test
    fun `fails when expected finding code is missing`() {
        val expected = HarnessExpectation(
            scenarioId = ScenarioId("test-scenario"),
            decision = LaunchDecision.CAUTION,
            severity = Severity.MEDIUM,
            expectedCategories = emptySet(),
            expectedFindingCodes = setOf(FindingCode("RATE_001"), FindingCode("DATA_001")),
        )

        val actual = HarnessActual(
            decision = LaunchDecision.CAUTION,
            severity = Severity.MEDIUM,
            categories = emptySet(),
            findingCodes = setOf(FindingCode("RATE_001")),
        )

        val result = comparator.compare(expected, actual)

        assertFalse(result.passed)
        assertTrue(result.mismatches.any { "finding" in it.lowercase() })
    }

    @Test
    fun `reports multiple mismatches`() {
        val expected = HarnessExpectation(
            scenarioId = ScenarioId("test-scenario"),
            decision = LaunchDecision.BLOCK,
            severity = Severity.HIGH,
            expectedCategories = setOf(RiskCategory.PROMPT_INJECTION),
            expectedFindingCodes = setOf(FindingCode("PI_001")),
        )

        val actual = HarnessActual(
            decision = LaunchDecision.ALLOW,
            severity = Severity.NONE,
            categories = emptySet(),
            findingCodes = emptySet(),
        )

        val result = comparator.compare(expected, actual)

        assertFalse(result.passed)
        assertTrue(result.mismatches.size >= 3)
    }
}
