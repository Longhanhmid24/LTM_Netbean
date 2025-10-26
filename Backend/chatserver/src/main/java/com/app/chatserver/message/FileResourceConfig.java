package com.app.chatserver.message;

import java.io.File;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Đường dẫn tuyệt đối đến thư mục uploads
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
        String uploadPath = "file:" + uploadDir + File.separator;
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(0); // Disable caching

    }
}
