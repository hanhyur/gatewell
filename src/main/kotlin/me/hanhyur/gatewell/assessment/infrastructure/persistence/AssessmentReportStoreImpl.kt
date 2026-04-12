package me.hanhyur.gatewell.assessment.infrastructure.persistence

import me.hanhyur.gatewell.assessment.domain.model.AssessmentId
import me.hanhyur.gatewell.assessment.domain.model.AssessmentReport
import me.hanhyur.gatewell.assessment.domain.port.AssessmentReportStore
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AssessmentReportStoreImpl(
    private val jpaRepository: JpaAssessmentReportRepository,
) : AssessmentReportStore {

    override fun save(report: AssessmentReport): AssessmentReport {
        val entity = AssessmentReportEntity.from(report)
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: AssessmentId): AssessmentReport? {
        return jpaRepository.findByIdOrNull(id.value)?.toDomain()
    }

    override fun findAll(): List<AssessmentReport> {
        return jpaRepository.findAll().map { it.toDomain() }
    }
}
