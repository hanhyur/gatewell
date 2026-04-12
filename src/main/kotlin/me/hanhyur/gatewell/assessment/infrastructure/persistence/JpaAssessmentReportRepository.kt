package me.hanhyur.gatewell.assessment.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaAssessmentReportRepository : JpaRepository<AssessmentReportEntity, UUID>
