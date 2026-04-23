package me.hanhyur.gatewell.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

@Component
class ApiKeyFilter(
    private val apiKeyRepository: ApiKeyRepository,
    @Value("\${gatewell.auth.enabled:true}")
    private val authEnabled: Boolean,
) : OncePerRequestFilter() {

    companion object {
        private const val API_KEY_HEADER = "X-API-Key"
        private val PUBLIC_PATHS = setOf(
            "/rule-version",
            "/scan",
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!authEnabled || isPublicPath(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        val apiKey = request.getHeader(API_KEY_HEADER)
        if (apiKey.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing X-API-Key header")
            return
        }

        val keyHash = ApiKeyEntity.hashKey(apiKey)
        val keyEntity = apiKeyRepository.findByKeyHashAndActiveTrue(keyHash)
        if (keyEntity == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key")
            return
        }

        if (keyEntity.expiresAt != null && keyEntity.expiresAt.isBefore(Instant.now())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key expired")
            return
        }

        request.setAttribute("apiKeyOwner", keyEntity.owner)
        request.setAttribute("apiKeyPlan", keyEntity.plan)
        filterChain.doFilter(request, response)
    }

    private fun isPublicPath(uri: String): Boolean =
        PUBLIC_PATHS.any { uri.startsWith(it) }
}
