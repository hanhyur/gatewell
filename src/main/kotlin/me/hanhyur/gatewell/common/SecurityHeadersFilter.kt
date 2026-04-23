package me.hanhyur.gatewell.common

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SecurityHeadersFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'")
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")

        filterChain.doFilter(request, response)
    }
}
