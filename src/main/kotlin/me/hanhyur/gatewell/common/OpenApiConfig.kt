package me.hanhyur.gatewell.common

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Gatewell API")
                .description("AI product launch risk evaluation service. Evaluate whether your AI-powered product is safe to launch.")
                .version("1.0.0")
                .contact(Contact().name("Gatewell").url("https://gatewell.dev"))
        )
        .components(
            Components()
                .addSecuritySchemes(
                    "ApiKey",
                    SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                )
        )
        .addSecurityItem(SecurityRequirement().addList("ApiKey"))
}
