package com.jc2001.campustrade.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    @Value("${app.storage.local-dir:./data/uploads}") private String uploadDir;
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location=Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/static/uploads/**").addResourceLocations(location);
    }
}
