package me.hanhyur.gatewell.assessment.api

import jakarta.validation.Valid
import me.hanhyur.gatewell.assessment.application.AssessmentService
import me.hanhyur.gatewell.assessment.domain.model.AssessmentId
import me.hanhyur.gatewell.assessment.domain.model.Evidence
import me.hanhyur.gatewell.assessment.domain.model.LaunchDecision
import me.hanhyur.gatewell.assessment.domain.model.Severity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/assessments")
class AssessmentController(
    private val assessmentService: AssessmentService,
) {

    @PostMapping
    fun createAssessment(@Valid @RequestBody request: AssessmentRequest): ResponseEntity<AssessmentResponse> {
        val command = request.toCommand()
        val report = assessmentService.assess(command)
        return ResponseEntity.ok(AssessmentResponse.from(report))
    }

    @GetMapping("/summary")
    fun getSummary(): ResponseEntity<DashboardSummary> {
        val reports = assessmentService.findAll(decision = null, severity = null)
        val allFindings = reports.flatMap { it.findings }

        val byDecision = LaunchDecision.entries.associateWith { d -> reports.count { it.decision == d } }
        val bySeverity = Severity.entries.associateWith { s -> reports.count { it.severity == s } }
        val topCategories = allFindings
            .groupingBy { it.category.name }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .associate { it.key to it.value }

        return ResponseEntity.ok(
            DashboardSummary(
                totalAssessments = reports.size,
                byDecision = byDecision.mapKeys { it.key.name },
                bySeverity = bySeverity.mapKeys { it.key.name },
                topCategories = topCategories,
            )
        )
    }

    @PostMapping("/{id}/reassess")
    fun reassess(
        @PathVariable id: String,
        @Valid @RequestBody request: ReassessRequest,
    ): ResponseEntity<ReassessResponse> {
        val assessmentId = AssessmentId(UUID.fromString(id))
        val newEvidences = request.evidences.map { Evidence(it) }
        val report = assessmentService.reassess(assessmentId, newEvidences)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ReassessResponse.from(id, report))
    }

    @GetMapping("/{id}/compare/{otherId}")
    fun compare(
        @PathVariable id: String,
        @PathVariable otherId: String,
    ): ResponseEntity<CompareResponse> {
        val before = assessmentService.findById(AssessmentId(UUID.fromString(id)))
            ?: return ResponseEntity.notFound().build()
        val after = assessmentService.findById(AssessmentId(UUID.fromString(otherId)))
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CompareResponse.from(before, after))
    }

    @GetMapping("/{id}")
    fun getAssessment(@PathVariable id: String): ResponseEntity<AssessmentResponse> {
        val assessmentId = AssessmentId(UUID.fromString(id))
        val report = assessmentService.findById(assessmentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AssessmentResponse.from(report))
    }

    @GetMapping
    fun listAssessments(
        @RequestParam(required = false) decision: String?,
        @RequestParam(required = false) severity: String?,
    ): ResponseEntity<List<AssessmentResponse>> {
        val decisionFilter = decision?.let { LaunchDecision.valueOf(it) }
        val severityFilter = severity?.let { Severity.valueOf(it) }
        val reports = assessmentService.findAll(decisionFilter, severityFilter)
        return ResponseEntity.ok(reports.map { AssessmentResponse.from(it) })
    }
}

data class DashboardSummary(
    val totalAssessments: Int,
    val byDecision: Map<String, Int>,
    val bySeverity: Map<String, Int>,
    val topCategories: Map<String, Int>,
)
