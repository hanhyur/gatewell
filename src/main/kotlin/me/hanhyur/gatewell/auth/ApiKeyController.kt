package me.hanhyur.gatewell.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
        if (secret != adminSecret) {
            return ResponseEntity.status(403).body(mapOf("error" to "Invalid admin secret"))
        }

        val key = "gw_${UUID.randomUUID().toString().replace("-", "")}"
        val entity = ApiKeyEntity(
            key = key,
            owner = request.owner,
            plan = request.plan ?: "free",
        )
        apiKeyRepository.save(entity)
        return ResponseEntity.ok(ApiKeyResponse(key = key, owner = entity.owner, plan = entity.plan))
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
)
