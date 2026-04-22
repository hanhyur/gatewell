package me.hanhyur.gatewell.scanner.persistence

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "email_leads")
class EmailLeadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true)
    val email: String,
    val clientIp: String,
    val createdAt: Instant = Instant.now(),
)
