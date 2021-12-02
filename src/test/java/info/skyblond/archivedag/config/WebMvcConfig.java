package info.skyblond.archivedag.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@TestConfiguration
@EnableSpringDataWebSupport
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageableHandlerMethodArgumentResolver());
    }
}
