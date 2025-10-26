package com.app.chatserver.message;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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

        // Đảm bảo thư mục uploads tồn tại
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // Tạo tên file duy nhất
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = timestamp + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path path = Paths.get(UPLOAD_DIR + fileName);

        // Lưu file vào thư mục uploads
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        // Lấy IP mạng LAN thực tế của máy server
        String serverIp = getLocalIp();
        String fileUrl = "http://" + serverIp + ":8080/" + UPLOAD_DIR + fileName;

        // Xác định loại file (image, video, file khác)
        String contentType = file.getContentType();
        String fileType = detectFileType(contentType);

        // Trả về JSON chi tiết
        Map<String, Object> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("fileName", fileName);
        response.put("fileType", fileType);
        response.put("sizeKB", file.getSize() / 1024);

        return ResponseEntity.ok(response);
    }

    // 🔍 Hàm tự động lấy IP LAN (ưu tiên IPv4 thật, tránh 127.0.0.1)
    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) continue;

                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "localhost"; // fallback
    }

    // Xác định loại file (ảnh, video, hoặc file)
    private String detectFileType(String mime) {
        if (mime == null) return "file";
        if (mime.startsWith("image/")) return "image";
        if (mime.startsWith("video/")) return "video";
        return "file";
    }
}
