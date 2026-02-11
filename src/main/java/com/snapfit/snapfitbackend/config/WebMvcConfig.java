package com.snapfit.snapfitbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "uploads" 폴더를 "/images/**" URL로 매핑
        // 예: http://localhost:8080/images/abc.jpg -> 프로젝트루트/uploads/abc.jpg
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/");
    }
}
