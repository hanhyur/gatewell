package me.hanhyur.gatewell.assessment.domain.port

import me.hanhyur.gatewell.assessment.domain.model.AssessmentId
import me.hanhyur.gatewell.assessment.domain.model.AssessmentReport

interface AssessmentReportStore {
    fun save(report: AssessmentReport): AssessmentReport
    fun findById(id: AssessmentId): AssessmentReport?
    fun findAll(): List<AssessmentReport>
}
