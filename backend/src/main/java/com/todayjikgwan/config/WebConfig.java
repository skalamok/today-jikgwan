package com.todayjikgwan.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 업로드된 이미지를 정적 리소스로 제공한다. 운영에서는 CDN/오브젝트 스토리지로 대체한다. */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final TodayJikgwanProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Paths.get(properties.storage().baseDir()).toAbsolutePath().normalize();
        registry.addResourceHandler(properties.storage().publicBaseUrl() + "/**")
                .addResourceLocations(base.toUri().toString());
    }
}
