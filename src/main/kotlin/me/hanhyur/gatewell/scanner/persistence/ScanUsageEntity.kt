package me.hanhyur.gatewell.scanner.persistence

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "scan_usage")
class ScanUsageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val clientIp: String,
    val scanDate: LocalDate = LocalDate.now(),
    val scanCount: Int = 1,
)
