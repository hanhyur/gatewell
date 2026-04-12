package me.hanhyur.gatewell.assessment.domain.model

import java.util.UUID

@JvmInline
value class AssessmentId(val value: UUID) {
    companion object {
        fun generate(): AssessmentId = AssessmentId(UUID.randomUUID())
    }
}
