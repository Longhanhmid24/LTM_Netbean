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
        if (originalName == null) originalName = "unknown";

        // Ép .jfif → .jpg
        if (originalName.toLowerCase().endsWith(".jfif")) {
            originalName = originalName.substring(0, originalName.length() - 5) + ".jpg";
        }

        String fileName = timestamp + "_" + StringUtils.cleanPath(originalName);
        Path path = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        // ✅ Xác định loại file (ảnh / video / file)
        String contentType = file.getContentType();
        String fileType = "file";
        if (contentType != null) {
            if (contentType.startsWith("image/")) fileType = "image";
            else if (contentType.startsWith("video/")) fileType = "video";
        }

        // ✅ Lấy IP LAN thật (VD: 192.168.1.230)
        String serverIp = getLocalIpAddress();
        String fileUrl = "http://" + serverIp + ":8080/uploads/" + fileName;

        // ✅ Trả JSON
        String jsonResponse = String.format(
                "{\"url\":\"%s\",\"fileName\":\"%s\",\"fileType\":\"%s\"}",
                fileUrl, fileName, fileType
        );

        return ResponseEntity.ok().body(jsonResponse);
    }

    // ✅ Tự động lấy IP LAN
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
