package me.hanhyur.gatewell.assessment.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AssessmentApiIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private val restClient: RestClient by lazy {
        RestClient.builder().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `POST assessments returns report with findings and decision`() {
        val request = mapOf(
            "productName" to "ChatBot Pro",
            "summary" to "AI chatbot with code execution",
            "evidences" to listOf("Input sanitization applied"),
            "capabilities" to listOf("CODE_EXECUTION"),
        )

        val response = restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, response.statusCode.value())
        val body = response.body!!
        assertEquals("ChatBot Pro", body["productName"])
        assertEquals("HIGH", body["severity"])
        assertEquals("BLOCK", body["launchDecision"])
        assertEquals("1.0.0", body["ruleVersion"])
        assertNotNull(body["recommendation"])

        @Suppress("UNCHECKED_CAST")
        val findings = body["findings"] as List<Map<String, Any>>
        assertTrue(findings.isNotEmpty())
        assertEquals("HIGH", findings[0]["severity"])
        assertEquals("AUTH_WEAKNESS", findings[0]["category"])

        @Suppress("UNCHECKED_CAST")
        val summary = body["findingsSummary"] as Map<String, Any>
        assertTrue((summary["total"] as Int) >= 1)
        assertTrue((summary["high"] as Int) >= 1)
        assertNotNull(summary["categories"])
    }

    @Test
    fun `POST assessments with no risky capabilities returns ALLOW`() {
        val request = mapOf(
            "productName" to "Simple Bot",
            "summary" to "Simple chatbot with no risky capabilities",
            "evidences" to listOf("Basic text only"),
            "capabilities" to emptyList<String>(),
        )

        val response = restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, response.statusCode.value())
        val body = response.body!!
        assertEquals("NONE", body["severity"])
        assertEquals("ALLOW", body["launchDecision"])

        @Suppress("UNCHECKED_CAST")
        val findings = body["findings"] as List<*>
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `POST assessments with blank productName returns 400`() {
        val request = mapOf(
            "productName" to "",
            "summary" to "test",
            "evidences" to listOf("evidence"),
            "capabilities" to emptyList<String>(),
        )

        try {
            restClient.post()
                .uri("/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(Map::class.java)
            throw AssertionError("Expected 400 Bad Request")
        } catch (ex: org.springframework.web.client.HttpClientErrorException) {
            assertEquals(400, ex.statusCode.value())
        }
    }

    @Test
    fun `POST assessments with empty evidences returns 400`() {
        val request = mapOf(
            "productName" to "Test Product",
            "summary" to "test",
            "evidences" to emptyList<String>(),
            "capabilities" to emptyList<String>(),
        )

        try {
            restClient.post()
                .uri("/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(Map::class.java)
            throw AssertionError("Expected 400 Bad Request")
        } catch (ex: org.springframework.web.client.HttpClientErrorException) {
            assertEquals(400, ex.statusCode.value())
        }
    }

    // --- GET /assessments/{id} ---

    @Test
    fun `GET assessments by id returns saved report`() {
        val request = mapOf(
            "productName" to "Retrievable Bot",
            "summary" to "Bot for retrieval test",
            "evidences" to listOf("Test evidence"),
            "capabilities" to listOf("FILE_ACCESS"),
        )

        val createResponse = restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(Map::class.java)

        val id = createResponse.body!!["id"] as String

        val getResponse = restClient.get()
            .uri("/assessments/$id")
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, getResponse.statusCode.value())
        val body = getResponse.body!!
        assertEquals("Retrievable Bot", body["productName"])
        assertEquals(id, body["id"])
    }

    @Test
    fun `GET assessments with unknown id returns 404`() {
        try {
            restClient.get()
                .uri("/assessments/00000000-0000-0000-0000-000000000000")
                .retrieve()
                .toEntity(Map::class.java)
            throw AssertionError("Expected 404 Not Found")
        } catch (ex: org.springframework.web.client.HttpClientErrorException) {
            assertEquals(404, ex.statusCode.value())
        }
    }

    // --- GET /assessments (list) ---

    @Test
    fun `GET assessments returns list of all reports`() {
        // Create two assessments
        restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "List Bot A",
                "summary" to "Bot A for list test",
                "evidences" to listOf("evidence A"),
                "capabilities" to listOf("CODE_EXECUTION"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "List Bot B",
                "summary" to "Bot B for list test",
                "evidences" to listOf("evidence B"),
                "capabilities" to emptyList<String>(),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val response = restClient.get()
            .uri("/assessments")
            .retrieve()
            .toEntity(List::class.java)

        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!.size >= 2)
    }

    @Test
    fun `GET assessments with decision filter returns matching reports`() {
        restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "Filter Block Bot",
                "summary" to "Bot with code execution",
                "evidences" to listOf("no mitigation"),
                "capabilities" to listOf("CODE_EXECUTION"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val response = restClient.get()
            .uri("/assessments?decision=BLOCK")
            .retrieve()
            .toEntity(List::class.java)

        assertEquals(200, response.statusCode.value())
        @Suppress("UNCHECKED_CAST")
        val reports = response.body!! as List<Map<String, Any>>
        assertTrue(reports.isNotEmpty())
        assertTrue(reports.all { it["launchDecision"] == "BLOCK" })
    }

    @Test
    fun `GET assessments with severity filter returns matching reports`() {
        restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "Filter Severity Bot",
                "summary" to "Safe bot",
                "evidences" to listOf("all good"),
                "capabilities" to emptyList<String>(),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val response = restClient.get()
            .uri("/assessments?severity=NONE")
            .retrieve()
            .toEntity(List::class.java)

        assertEquals(200, response.statusCode.value())
        @Suppress("UNCHECKED_CAST")
        val reports = response.body!! as List<Map<String, Any>>
        assertTrue(reports.isNotEmpty())
        assertTrue(reports.all { it["severity"] == "NONE" })
    }

    // --- GET /assessments/summary ---

    @Test
    fun `GET assessments summary returns aggregate stats`() {
        // Ensure at least one assessment exists
        restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "Summary Test Bot",
                "summary" to "Bot for summary test",
                "evidences" to listOf("test"),
                "capabilities" to listOf("CODE_EXECUTION"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val response = restClient.get()
            .uri("/assessments/summary")
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, response.statusCode.value())
        val body = response.body!!
        assertTrue((body["totalAssessments"] as Int) >= 1)
        assertNotNull(body["byDecision"])
        assertNotNull(body["bySeverity"])
        assertNotNull(body["topCategories"])
    }

    // --- GET /rule-version ---

    @Test
    fun `GET rule-version returns current rule info`() {
        val response = restClient.get()
            .uri("/rule-version")
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, response.statusCode.value())
        val body = response.body!!
        assertEquals("1.0.0", body["version"])
        assertTrue((body["totalRules"] as Int) >= 11)
        assertNotNull(body["rules"])
    }

    // --- POST /assessments/{id}/reassess ---

    @Test
    fun `POST reassess with new evidence improves the score`() {
        // Create initial assessment with CODE_EXECUTION → BLOCK
        val createResponse = restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "Reassess Bot",
                "summary" to "Bot with code execution",
                "evidences" to listOf("no mitigation"),
                "capabilities" to listOf("CODE_EXECUTION"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val originalId = createResponse.body!!["id"] as String
        assertEquals("BLOCK", createResponse.body!!["launchDecision"])

        // Reassess with sandbox evidence → should improve to CAUTION
        val reassessResponse = restClient.post()
            .uri("/assessments/$originalId/reassess")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "evidences" to listOf("Code execution runs in sandboxed Docker container"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, reassessResponse.statusCode.value())
        val body = reassessResponse.body!!
        assertEquals("CAUTION", body["launchDecision"])
        assertEquals("Reassess Bot", body["productName"])
        assertNotNull(body["previousAssessmentId"])
        assertEquals(originalId, body["previousAssessmentId"])
    }

    // --- GET /assessments/{id}/compare/{otherId} ---

    @Test
    fun `GET compare returns diff between two assessments`() {
        val first = restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "Compare Bot",
                "summary" to "Bot with code execution",
                "evidences" to listOf("no mitigation"),
                "capabilities" to listOf("CODE_EXECUTION"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val second = restClient.post()
            .uri("/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf(
                "productName" to "Compare Bot",
                "summary" to "Bot with code execution",
                "evidences" to listOf("Code runs in sandboxed container"),
                "capabilities" to listOf("CODE_EXECUTION"),
            ))
            .retrieve()
            .toEntity(Map::class.java)

        val id1 = first.body!!["id"] as String
        val id2 = second.body!!["id"] as String

        val response = restClient.get()
            .uri("/assessments/$id1/compare/$id2")
            .retrieve()
            .toEntity(Map::class.java)

        assertEquals(200, response.statusCode.value())
        val body = response.body!!
        assertNotNull(body["before"])
        assertNotNull(body["after"])
        assertNotNull(body["decisionChanged"])
        assertNotNull(body["severityChanged"])
        assertNotNull(body["resolvedFindings"])
        assertNotNull(body["newFindings"])
    }
}
