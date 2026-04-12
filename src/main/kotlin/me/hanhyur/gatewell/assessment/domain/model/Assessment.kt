package me.hanhyur.gatewell.assessment.domain.model

data class Assessment(
    val id: AssessmentId,
    val productName: ProductName,
    val summary: String,
    val evidences: List<Evidence>,
    val capabilities: Set<Capability>,
)
