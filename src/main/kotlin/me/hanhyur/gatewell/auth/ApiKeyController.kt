package me.hanhyur.gatewell.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/api-keys")
class ApiKeyController(
    private val apiKeyRepository: ApiKeyRepository,
    @Value("\${gatewell.admin.secret:}")
    private val adminSecret: String,
) {

    @PostMapping
    fun createApiKey(
        @RequestHeader("X-Admin-Secret") secret: String?,
        @RequestBody request: CreateApiKeyRequest,
    ): ResponseEntity<Any> {
        if (adminSecret.isBlank()) {
            return ResponseEntity.status(503).body(mapOf("error" to "Admin secret not configured"))
        }
        if (!constantTimeEquals(secret ?: "", adminSecret)) {
            return ResponseEntity.status(403).body(mapOf("error" to "Invalid admin secret"))
        }

        val rawKey = "gw_${UUID.randomUUID().toString().replace("-", "")}"
        val entity = ApiKeyEntity(
            keyHash = ApiKeyEntity.hashKey(rawKey),
            keyPrefix = ApiKeyEntity.prefixOf(rawKey),
            owner = request.owner,
            plan = request.plan ?: "free",
            expiresAt = Instant.now().plus(90, ChronoUnit.DAYS),
        )
        apiKeyRepository.save(entity)

        return ResponseEntity.ok(ApiKeyResponse(
            key = rawKey,
            owner = entity.owner,
            plan = entity.plan,
            expiresAt = entity.expiresAt?.toString(),
            note = "Save this key now. It cannot be retrieved again.",
        ))
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }
}

data class CreateApiKeyRequest(
    val owner: String,
    val plan: String? = null,
)

data class ApiKeyResponse(
    val key: String,
    val owner: String,
    val plan: String,
    val expiresAt: String?,
    val note: String,
)
