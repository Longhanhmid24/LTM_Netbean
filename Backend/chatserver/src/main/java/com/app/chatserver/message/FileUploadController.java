package com.app.chatserver.message;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File rỗng");
        }

        // Đảm bảo thư mục tồn tại
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // Đặt tên file duy nhất
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = timestamp + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path path = Paths.get(UPLOAD_DIR + fileName);

        // Lưu file
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "http://localhost:8080/" + UPLOAD_DIR + fileName;

        return ResponseEntity.ok().body(fileUrl);
    }
}
