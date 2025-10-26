package com.app.chatserver.media_file;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Lấy đường dẫn tuyệt đối tới thư mục uploads trong project
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        System.out.println("✅ Serving static files from: file:" + uploadPath + "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
