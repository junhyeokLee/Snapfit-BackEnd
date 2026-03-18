package com.snapfit.snapfitbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${snapfit.storage.local-dir:/tmp/snapfit-uploads}")
    private String localStorageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 저장 폴더를 "/images/**" URL로 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + ensureTrailingSlash(localStorageDir));
    }

    private String ensureTrailingSlash(String path) {
        if (path == null || path.isBlank()) {
            return "/tmp/snapfit-uploads/";
        }
        return path.endsWith("/") ? path : path + "/";
    }
}
