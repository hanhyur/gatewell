package me.hanhyur.gatewell.auth

import org.springframework.data.jpa.repository.JpaRepository

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, String> {
    fun findByKeyHashAndActiveTrue(keyHash: String): ApiKeyEntity?
}
