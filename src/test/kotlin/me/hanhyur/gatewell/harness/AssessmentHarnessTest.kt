package me.hanhyur.gatewell.harness

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import me.hanhyur.gatewell.assessment.application.AssessmentCommand
import me.hanhyur.gatewell.assessment.domain.model.*
import me.hanhyur.gatewell.assessment.domain.policy.LaunchDecisionPolicy
import me.hanhyur.gatewell.assessment.domain.policy.RiskScoringEngine
import me.hanhyur.gatewell.harness.domain.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertTrue

class AssessmentHarnessTest {

    private val engine = RiskScoringEngine()
    private val policy = LaunchDecisionPolicy()
    private val comparator = HarnessComparator()
    private val objectMapper = jacksonObjectMapper()

    @ParameterizedTest(name = "harness scenario: {0}")
    @ValueSource(strings = [
        "prompt-injection-basic",
        "missing-rate-limit",
        "vibe-coded-full-risk",
        "safe-text-only-bot",
        "partial-mitigation",
        "harmful-output-unfiltered",
        "production-no-observability",
        "code-exec-web-browse-combo",
        "db-user-data-combo",
    ])
    fun `harness regression test`(scenarioName: String) {
        val fixture = loadFixture(scenarioName)

        val command = AssessmentCommand(
            productName = ProductName(fixture.input.productName),
            summary = fixture.input.summary,
            evidences = fixture.input.evidences.map { Evidence(it) },
            capabilities = fixture.input.capabilities.map { Capability.valueOf(it) }.toSet(),
            ruleVersion = RuleVersion(fixture.ruleVersion),
        )

        val scoringResult = engine.evaluate(command)
        val decisionResult = policy.decide(scoringResult)

        val expected = HarnessExpectation(
            scenarioId = ScenarioId(fixture.scenarioId),
            decision = LaunchDecision.valueOf(fixture.expected.decision),
            severity = Severity.valueOf(fixture.expected.severity),
            expectedCategories = fixture.expected.categories.map { RiskCategory.valueOf(it) }.toSet(),
            expectedFindingCodes = fixture.expected.findingCodes.map { FindingCode(it) }.toSet(),
        )

        val actual = HarnessActual(
            decision = decisionResult.decision,
            severity = scoringResult.severity,
            categories = scoringResult.findings.map { it.category }.toSet(),
            findingCodes = scoringResult.findings.map { it.code }.toSet(),
        )

        val result = comparator.compare(expected, actual)

        assertTrue(
            result.passed,
            "Harness scenario '${fixture.scenarioId}' failed.\nMismatches:\n${result.mismatches.joinToString("\n") { "  - $it" }}",
        )
    }

    private fun loadFixture(name: String): HarnessFixture {
        val stream = javaClass.classLoader.getResourceAsStream("harness/$name.json")
            ?: throw IllegalStateException("Fixture not found: harness/$name.json")
        return objectMapper.readValue(stream)
    }
}

data class HarnessFixture(
    val scenarioId: String,
    val scenarioVersion: String,
    val ruleVersion: String,
    val description: String,
    val input: HarnessInput,
    val expected: HarnessExpected,
)

data class HarnessInput(
    val productName: String,
    val summary: String,
    val evidences: List<String>,
    val capabilities: List<String>,
)

data class HarnessExpected(
    val decision: String,
    val severity: String,
    val categories: List<String>,
    val findingCodes: List<String>,
)
