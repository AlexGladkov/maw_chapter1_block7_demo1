package tech.mobiledeveloper.server.configuration

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class SerializationConfig : WebMvcConfigurer {

    @Bean
    fun kotlinxJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }

    @Bean
    fun kotlinxConverter(json: Json) = KotlinSerializationJsonHttpMessageConverter(json)

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // ставим наш конвертер первым по приоритету
        val json = kotlinxJson()
        converters.add(0, KotlinSerializationJsonHttpMessageConverter(json))
    }
}