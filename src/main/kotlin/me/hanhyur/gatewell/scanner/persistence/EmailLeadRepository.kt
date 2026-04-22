package me.hanhyur.gatewell.scanner.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface EmailLeadRepository : JpaRepository<EmailLeadEntity, Long> {
    fun existsByEmail(email: String): Boolean
}
