package com.app.chatserver.media_file;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "*")
public class MediaUploadController {

    @PostMapping("/upload")
    public Map<String, Object> uploadMedia(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không có file nào được chọn!");
            return response;
        }

        // 📂 Thư mục lưu file (trùng với cấu hình trong FileResourceConfig)
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        // 🧾 Đặt tên file duy nhất (thêm timestamp)
        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = Paths.get(uploadDir, fileName);

        // 💾 Lưu file vào thư mục uploads/
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 🔍 Xác định loại file (ảnh, video, file khác)
        String fileType = detectFileType(originalFileName);

        // 🌐 Tạo URL truy cập file
        String fileUrl = "/uploads/" + fileName;

        response.put("success", true);
        response.put("message", "Tải lên thành công!");
        response.put("fileName", originalFileName);
        response.put("url", fileUrl);
        response.put("type", fileType);

        return response;
    }

    // 📌 Hàm xác định loại file dựa trên đuôi
    private String detectFileType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")) {
            return "image";
        } else if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".mkv")) {
            return "video";
        } else if (lower.endsWith(".mp3") || lower.endsWith(".wav")) {
            return "audio";
        } else {
            return "file";
        }
    }
}
