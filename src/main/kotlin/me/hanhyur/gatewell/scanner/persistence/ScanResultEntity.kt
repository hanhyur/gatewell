package me.hanhyur.gatewell.scanner.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "scan_results")
class ScanResultEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    val scanType: String,
    val target: String,
    val reachable: Boolean,
    val decision: String,
    val totalFindings: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val infoCount: Int,
    @Column(length = 1000)
    val categories: String,
    @Column(columnDefinition = "TEXT")
    val findingsJson: String,
    val createdAt: Instant = Instant.now(),
)
