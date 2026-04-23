package me.hanhyur.gatewell.auth

import jakarta.persistence.*
import java.security.MessageDigest
import java.time.Instant

@Entity
@Table(name = "api_keys")
class ApiKeyEntity(
    @Id
    val keyHash: String,
    val keyPrefix: String,
    val owner: String,
    val plan: String = "free",
    val active: Boolean = true,
    val expiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
) {
    companion object {
        fun hashKey(rawKey: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(rawKey.toByteArray()).joinToString("") { "%02x".format(it) }
        }

        fun prefixOf(rawKey: String): String = rawKey.take(7)
    }
}
