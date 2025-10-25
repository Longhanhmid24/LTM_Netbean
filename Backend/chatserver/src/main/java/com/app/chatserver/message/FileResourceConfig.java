package com.app.chatserver.message;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cho phép truy cập các file trong thư mục /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/"); // đường dẫn thật trên server
    }
}
