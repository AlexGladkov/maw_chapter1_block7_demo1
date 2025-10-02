package tech.mobiledeveloper.server.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class WebConfig(
    @Value("\${app.allow-origins:*}") private val allowOrigins: String
) {
    @Bean
    fun corsFilter(): CorsFilter {
        val cfg = CorsConfiguration().apply {
            allowCredentials = true
            allowOrigins.split(",").map { it.trim() }.forEach { addAllowedOriginPattern(it) }
            addAllowedHeader("*")
            addAllowedMethod("*")
        }
        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", cfg)
        }
        return CorsFilter(source)
    }
}