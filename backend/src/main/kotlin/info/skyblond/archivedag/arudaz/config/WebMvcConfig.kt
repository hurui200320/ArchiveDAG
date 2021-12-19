package info.skyblond.archivedag.arudaz.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
class WebMvcConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedMethods("*")
            .allowedHeaders("*")
    }

    override fun extendMessageConverters(converters: List<HttpMessageConverter<*>>) {
        // Add modules to Jackson's ObjectMapper
        converters.stream().filter { it is MappingJackson2HttpMessageConverter }
            .forEach { converter: HttpMessageConverter<*> ->
                val jsonConverter = converter as MappingJackson2HttpMessageConverter
                jsonConverter.objectMapper.findAndRegisterModules()
            }
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        // Add parameter resolver for pageable
        resolvers.add(PageableHandlerMethodArgumentResolver())
    }
}
