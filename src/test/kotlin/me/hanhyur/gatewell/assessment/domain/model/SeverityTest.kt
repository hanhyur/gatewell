package me.hanhyur.gatewell.assessment.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SeverityTest {

    @Test
    fun `from returns NONE when findings list is empty`() {
        val result = Severity.from(emptyList())
        assertEquals(Severity.NONE, result)
    }

    @Test
    fun `from returns LOW when only LOW findings exist`() {
        val findings = listOf(
            Finding.low(
                category = RiskCategory.OBSERVABILITY_GAPS,
                code = FindingCode("OBS_001"),
                message = "Minor observability gap"
            )
        )
        assertEquals(Severity.LOW, Severity.from(findings))
    }

    @Test
    fun `from returns MEDIUM when MEDIUM is highest severity`() {
        val findings = listOf(
            Finding.low(
                category = RiskCategory.OBSERVABILITY_GAPS,
                code = FindingCode("OBS_001"),
                message = "Minor observability gap"
            ),
            Finding.medium(
                category = RiskCategory.RATE_LIMITING,
                code = FindingCode("RATE_001"),
                message = "Rate limiting not configured"
            )
        )
        assertEquals(Severity.MEDIUM, Severity.from(findings))
    }

    @Test
    fun `from returns HIGH when any HIGH finding exists`() {
        val findings = listOf(
            Finding.low(
                category = RiskCategory.OBSERVABILITY_GAPS,
                code = FindingCode("OBS_001"),
                message = "Minor gap"
            ),
            Finding.medium(
                category = RiskCategory.RATE_LIMITING,
                code = FindingCode("RATE_001"),
                message = "Rate limit issue"
            ),
            Finding.high(
                category = RiskCategory.PROMPT_INJECTION,
                code = FindingCode("PI_001"),
                message = "Prompt injection vulnerability"
            )
        )
        assertEquals(Severity.HIGH, Severity.from(findings))
    }

    @Test
    fun `severity ordinal order is NONE, LOW, MEDIUM, HIGH`() {
        val sorted = Severity.entries.sortedBy { it.ordinal }
        assertEquals(listOf(Severity.NONE, Severity.LOW, Severity.MEDIUM, Severity.HIGH), sorted)
    }
}

class FindingTest {

    @Test
    fun `high factory creates HIGH severity finding`() {
        val finding = Finding.high(
            category = RiskCategory.PROMPT_INJECTION,
            code = FindingCode("PI_001"),
            message = "Prompt injection detected"
        )
        assertEquals(Severity.HIGH, finding.severity)
        assertEquals(RiskCategory.PROMPT_INJECTION, finding.category)
        assertEquals(FindingCode("PI_001"), finding.code)
    }

    @Test
    fun `medium factory creates MEDIUM severity finding`() {
        val finding = Finding.medium(
            category = RiskCategory.ABUSE_SPAM,
            code = FindingCode("ABUSE_001"),
            message = "Abuse potential"
        )
        assertEquals(Severity.MEDIUM, finding.severity)
    }

    @Test
    fun `low factory creates LOW severity finding`() {
        val finding = Finding.low(
            category = RiskCategory.OBSERVABILITY_GAPS,
            code = FindingCode("OBS_001"),
            message = "Minor gap"
        )
        assertEquals(Severity.LOW, finding.severity)
    }
}

class ValueObjectValidationTest {

    @Test
    fun `ProductName rejects blank value`() {
        assertThrows<IllegalArgumentException> { ProductName("") }
        assertThrows<IllegalArgumentException> { ProductName("   ") }
    }

    @Test
    fun `ProductName accepts valid value`() {
        val name = ProductName("My AI Product")
        assertEquals("My AI Product", name.value)
    }

    @Test
    fun `FindingCode rejects blank value`() {
        assertThrows<IllegalArgumentException> { FindingCode("") }
    }

    @Test
    fun `FindingCode accepts valid value`() {
        val code = FindingCode("PI_001")
        assertEquals("PI_001", code.value)
    }

    @Test
    fun `Evidence rejects blank value`() {
        assertThrows<IllegalArgumentException> { Evidence("") }
    }

    @Test
    fun `Evidence accepts valid value`() {
        val evidence = Evidence("User input is sanitized via DOMPurify")
        assertEquals("User input is sanitized via DOMPurify", evidence.value)
    }

    @Test
    fun `RuleVersion rejects blank value`() {
        assertThrows<IllegalArgumentException> { RuleVersion("") }
    }

    @Test
    fun `RuleVersion accepts valid value`() {
        val version = RuleVersion("1.0.0")
        assertEquals("1.0.0", version.value)
    }
}
