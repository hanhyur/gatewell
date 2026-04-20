package me.hanhyur.gatewell.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api-keys")
class ApiKeyController(
    private val apiKeyRepository: ApiKeyRepository,
) {

    @PostMapping
    fun createApiKey(@RequestBody request: CreateApiKeyRequest): ResponseEntity<ApiKeyResponse> {
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
