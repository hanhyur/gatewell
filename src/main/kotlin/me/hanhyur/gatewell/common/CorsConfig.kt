package me.hanhyur.gatewell.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig(
    @Value("\${gatewell.cors.allowed-origins:http://localhost:3000}")
    private val allowedOrigins: String,
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration()
        allowedOrigins.split(",").forEach { config.addAllowedOrigin(it.trim()) }
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}
