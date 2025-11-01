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
    String uploadLocation = uploadDir.toUri().toString(); // e.g. file:///D:/.../uploads/

    System.out.println("✅ Serving static files from: " + uploadLocation);

    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploadLocation)
        .setCachePeriod(0);
    }
}
