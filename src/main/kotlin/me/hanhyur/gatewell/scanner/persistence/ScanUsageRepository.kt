package me.hanhyur.gatewell.scanner.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ScanUsageRepository : JpaRepository<ScanUsageEntity, Long> {
    fun findByClientIpAndScanDate(clientIp: String, scanDate: LocalDate): ScanUsageEntity?
}
