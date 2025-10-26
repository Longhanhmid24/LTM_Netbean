package com.app.chatserver.message;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

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

        // Tạo thư mục nếu chưa có
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // Đặt tên file duy nhất
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "unknown";
        }
        String fileName = timestamp + "_" + StringUtils.cleanPath(originalName);
        Path path = Paths.get(UPLOAD_DIR + fileName);

        // Lưu file
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        // Sử dụng IP của máy thật nơi chạy server
        String fileUrl = "http://192.168.1.230:8080/uploads/" + fileName;

        return ResponseEntity.ok().body("{\"url\":\"" + fileUrl + "\",\"fileName\":\"" + fileName + "\"}");
    }

private String getLocalIpAddress() {
    try {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        }
    } catch (SocketException e) {
        e.printStackTrace();
    }
    return "localhost";
}
}
