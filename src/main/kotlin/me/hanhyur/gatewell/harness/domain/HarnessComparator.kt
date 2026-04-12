package me.hanhyur.gatewell.harness.domain

class HarnessComparator {

    fun compare(expected: HarnessExpectation, actual: HarnessActual): HarnessResult {
        val mismatches = mutableListOf<String>()

        if (expected.decision != actual.decision) {
            mismatches += "Decision mismatch: expected=${expected.decision}, actual=${actual.decision}"
        }

        if (expected.severity != actual.severity) {
            mismatches += "Severity mismatch: expected=${expected.severity}, actual=${actual.severity}"
        }

        val missingCategories = expected.expectedCategories - actual.categories
        if (missingCategories.isNotEmpty()) {
            mismatches += "Missing categories: $missingCategories"
        }

        val missingFindingCodes = expected.expectedFindingCodes - actual.findingCodes
        if (missingFindingCodes.isNotEmpty()) {
            mismatches += "Missing finding codes: ${missingFindingCodes.map { it.value }}"
        }

        return HarnessResult(
            scenarioId = expected.scenarioId,
            expectedDecision = expected.decision,
            actualDecision = actual.decision,
            passed = mismatches.isEmpty(),
            mismatches = mismatches.toList(),
        )
    }
}
