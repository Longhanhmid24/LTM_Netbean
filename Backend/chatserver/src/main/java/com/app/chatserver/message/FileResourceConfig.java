package com.app.chatserver.message;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối tới thư mục uploads (dù chạy ở đâu cũng đúng)
        String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
