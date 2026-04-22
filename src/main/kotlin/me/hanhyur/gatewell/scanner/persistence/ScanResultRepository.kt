package me.hanhyur.gatewell.scanner.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ScanResultRepository : JpaRepository<ScanResultEntity, UUID>
