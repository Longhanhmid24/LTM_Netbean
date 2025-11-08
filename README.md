# LTM_Netbean  
**Ứng dụng SuperChat mã hóa E2EE**

> Một ứng dụng chat thời gian thực, bảo mật cao với **mã hóa đầu-cuối (E2EE)** cho tin nhắn 1:1, hỗ trợ chat nhóm, gửi file đa phương tiện, gọi thoại/video 1:1 qua **trang web HTML + WebRTC**.  
>  
> **Backend**: Java Spring Boot + WebSocket/STOMP  
> **Frontend**: Java Desktop (Swing) + WebView (HTML/JS cho gọi thoại/video)

---

## Tính năng chính

| Tính năng                  | Trạng thái | Ghi chú |
|----------------------------|------------|--------|
| Chat 1:1 (E2EE)            | Done       | RSA + AES-GCM |
| Chat nhóm                  | Done       | Chưa có E2EE (chỉ TLS) |
| Gửi ảnh/video/file         | Done       | Mã hóa + upload server |
| Xem ảnh                    | Done       | Image viewer tích hợp |
| Phát video/âm thanh        | Done       | Dùng **VLCj** (yêu cầu VLC) |
| Gọi thoại/video 1:1        | Done       | Dùng **WebRTC + trang HTML** |
| Gọi nhóm                   | Not Done   | Chưa triển khai |
| Quản lý bạn bè             | Done       | Yêu cầu, chấp nhận, từ chối |
| Quản lý nhóm               | Done       | Tạo, thêm thành viên, xóa |
| Tìm kiếm, chặn người dùng  | Done       | Trong danh bạ |
| Thông báo đẩy              | Done       | Real-time qua STOMP |

> **Lưu ý**: **Trạng thái online/offline chưa được triển khai**.

---

## 📦 Danh sách thư viện (Dependencies)

| Thư viện | Phiên bản |
|---------|----------|
| `Java-WebSocket` | 1.5.5 |
| `common-image` | 3.10.1 |
| `common-io` | 3.10.1 |
| `common-lang3` | 3.10.1 |
| `imageio-core` | 3.10.1 |
| `imageio-metadata` | 3.10.1 |
| `imageio-webp` | 3.10.1 |
| `jackson-annotations` | 2.17.2 |
| `jackson-core` | 2.17.2 |
| `jackson-databind` | 2.17.2 |
| `jackson-datatype-jsr310` | 2.17.2 |
| `jna-jpms` | 5.14.0 |
| `jna-platform-jpms` | 5.14.0 |
| `slf4j-api` | 2.0.9 |
| `slf4j-simple` | 2.0.9 |
| `spring-messaging` | 6.1.15 |
| `spring-websocket` | 6.1.15 |
| `vlcj` | 4.11.0 |
| `vlcj-natives` | 4.8.3 |

> **Yêu cầu hệ thống**:
> - **VLC Media Player 64-bit** (để phát video/âm thanh)
> - **Trình duyệt hiện đại** (Chrome/Firefox) để **cho phép quyền truy cập camera & micro** khi gọi

---

## 🏗 Kiến trúc hệ thống (3 lớp chính)

### 1. **Lớp Giao tiếp mạng (Networking Layer)**

1. Đảm nhận việc thiết lập và duy trì kết nối hai chiều giữa client và server thông qua giao thức **WebSocket**.
2. Server được phát triển bằng **Spring Framework (WebSocket & STOMP)**, có khả năng:
   - Lắng nghe và xử lý nhiều kết nối đồng thời (**multi-threaded**).
   - Gửi/nhận thông điệp tức thời với độ trễ thấp.
3. Mỗi client khi kết nối sẽ được gán một **phiên socket riêng**, đảm bảo dữ liệu truyền đi đúng đối tượng nhận mà không bị gián đoạn.
4. Nhờ sử dụng **STOMP protocol**, hệ thống có thể định tuyến tin nhắn theo chủ đề hoặc phòng chat (**topic-based messaging**).

---

### 2. **Lớp Bảo mật (Security Layer)**

1. Là lớp quan trọng nhất trong hệ thống, chịu trách nhiệm bảo vệ toàn bộ dữ liệu người dùng.
2. Ứng dụng áp dụng kết hợp hai cơ chế mã hóa:
   - **Mã hóa đầu-cuối (End-to-End Encryption – E2EE)**:
     - Mỗi người dùng sở hữu cặp khóa **RSA** gồm **khóa công khai (public key)** và **khóa riêng (private key)**.
     - Khi gửi tin nhắn, nội dung được mã hóa bằng **AES-GCM session key**, sau đó session key được mã hóa lại bằng **khóa công khai của người nhận**.
     - Kết quả là **chỉ người nhận** mới có thể giải mã nội dung bằng khóa riêng của họ.
     - **Private key** được **mã hóa bằng mật khẩu người dùng** (PBKDF2 + AES-GCM) và lưu trong DB.
   - **Mã hóa đường truyền (TLS/SSL)**:
     - Toàn bộ kết nối giữa client và server đều truyền qua **HTTPS** hoặc **WSS (WebSocket Secure)** để chống nghe lén hoặc tấn công trung gian (**Man-in-the-Middle**).

---

### 3. **Lớp Ứng dụng (Application Layer)**

1. Là lớp xử lý logic nghiệp vụ và tương tác người dùng.
2. Bao gồm các chức năng chính:
   - Đăng ký / Đăng nhập người dùng (với mã hóa mật khẩu và quản lý khóa RSA).
   - Nhắn tin cá nhân và nhóm theo thời gian thực.
   - Gửi / nhận file đa phương tiện (ảnh, video, âm thanh).
   - **Gọi âm thanh / video (qua module WebRTC)**.
   - Thông báo đẩy.
3. Server chịu trách nhiệm:
   - Xác thực, phân quyền và định tuyến thông điệp.
   - Gửi broadcast đến người nhận tương ứng (theo ID hoặc nhóm).
   - Lưu trữ **bản mã tin nhắn** và **khóa mã hóa tương ứng** vào cơ sở dữ liệu (`messages`, `message_keys`).
4. Client là ứng dụng **Java desktop** (sử dụng **Java-WebSocket** và **VLCj**), hiển thị giao diện chat, quản lý bạn bè và thực hiện cuộc gọi trực tiếp.

---

## ⚙ Quy trình hoạt động và triển khai

### 1. **Khởi động hệ thống**
1. Máy chủ (**Server**) được khởi chạy trên nền **Spring Boot**, lắng nghe kết nối WebSocket tại:
   - Cổng mặc định: `8080` (HTTP)
   - Cổng bảo mật: `8443` (WSS)
2. Các client khởi tạo kết nối WebSocket đến **địa chỉ IP của server**.

### 2. **Xác thực và bắt tay bảo mật (Handshake)**
1. Khi client kết nối, server thực hiện **TLS Handshake** để thiết lập kênh truyền bảo mật.
2. Sau khi xác thực thành công, client và server **trao đổi khóa công khai RSA** để phục vụ mã hóa đầu-cuối.

### 3. **Trao đổi tin nhắn**
1. Khi người dùng gửi tin nhắn, ứng dụng client sẽ:
   1. Tạo **khóa AES-GCM ngẫu nhiên**.
   2. Mã hóa nội dung bằng **khóa AES-GCM**.
   3. Mã hóa **khóa AES** bằng **khóa công khai RSA của người nhận**.
   4. Gửi dữ liệu mã hóa qua **WebSocket** đến server.
2. Server **chỉ đóng vai trò định tuyến (forward)** tin nhắn đã mã hóa đến đúng người nhận.

### 4. **Nhận và giải mã**
1. Client nhận tin nhắn → giải mã **AES key** bằng **khóa riêng RSA** (được giải mã từ mật khẩu) → giải mã nội dung → hiển thị.

### 5. **Đồng bộ và lưu trữ**
1. Server lưu **bản mã của tin nhắn** và **thông tin khóa tương ứng**.
2. Các client có thể **đồng bộ lịch sử tin nhắn** khi đăng nhập lại.

---

## ✅ Tác vụ cơ bản

### 1. **Tác vụ đăng ký tài khoản**
1. Người dùng nhập thông tin (tên, email, số điện thoại, mật khẩu).
2. Kiểm tra trùng lặp và lưu thông tin **mã hóa** vào cơ sở dữ liệu.
3. Client nhận thông báo **đăng ký thành công** hoặc **lỗi** (ví dụ: tài khoản đã tồn tại).

### 2. **Tác vụ đăng nhập**
1. Gửi thông tin đăng nhập (username, mật khẩu).
2. Giải mã mật khẩu và so sánh với thông tin trong DB → **xác nhận** → tạo **phiên làm việc (session)**.
3. Người dùng có thể bắt đầu sử dụng các chức năng.

### 3. **Tác vụ nhắn tin cá nhân**
1. Người gửi: Tin nhắn được **mã hóa bằng E2EE** trước khi gửi qua WebSocket.
2. Server nhận gói tin mã hóa và **chuyển tiếp đến người nhận** mà **không giải mã**.
3. Người nhận: **Giải mã E2EE** và xem nội dung gốc.
4. Tin được hiển thị đầy đủ, đảm bảo **End-to-End Encryption**.

### 4. **Tác vụ gửi tệp đa phương tiện**
1. **Ảnh / Video / Âm thanh / Tài liệu**: Người dùng chọn tệp gửi qua chat.
2. Ứng dụng sẽ:
   - **Mã hóa file**
   - **Upload lên server**
   - Gửi **đường dẫn an toàn** đến người nhận
3. **Xem trước file**: Sử dụng **VLCj** để phát nhạc, xem video trực tiếp trong ứng dụng.
4. **Giới hạn kích thước**: Chỉ cho phép tệp dưới dung lượng nhất định (cấu hình được).

### 5. **Tác vụ quản lý bạn bè**
1. Người dùng gửi **yêu cầu kết bạn** đến một tài khoản khác.
2. Server tiếp nhận và gửi **thông báo** đến người nhận yêu cầu.
3. Người nhận có thể **chấp nhận** hoặc **từ chối**.
4. Khi hai bên đồng ý, mối quan hệ bạn bè được lưu vào DB → cả hai có thể nhắn tin.

### 6. **Bảo mật và mã hóa**
1. **Mã hóa đầu cuối E2EE**.
2. Dữ liệu được mã hóa bằng **RSA / AES-GCM** trước khi gửi qua mạng.
3. **Xác thực phiên làm việc**.
4. Sử dụng **JWT** để đảm bảo chỉ người dùng hợp lệ mới gửi/nhận dữ liệu.
5. **Lưu trữ an toàn**: Server chỉ lưu **bản mã**, **không thể đọc được tin nhắn gốc**.

### 7. **Tác vụ quản lý danh bạ**
1. Hiển thị **danh sách bạn bè** từ cơ sở dữ liệu, bao gồm tên.
2. Có thể **tìm kiếm bạn bè**, **xóa** người dùng khác nếu cần.

---

## Gọi thoại & video (WebRTC)

- **Giao diện gọi**: Dùng **trang HTML** (`call.html`) được nhúng trong **Java WebView**.
- **Yêu cầu**:
  - Người dùng **phải cho phép quyền truy cập camera & micro** trong trình duyệt mặc định của người dùng.
  - Nếu dùng **HTTP (cổng 8080)** → cần **Allow Insecure Content** trong trình duyệt.
  - **Khuyến nghị**: Dùng **WSS + HTTPS (cổng 8443)** để tránh cảnh báo.

## Cấu hình trình duyệt để dùng camera & micro (khi dùng HTTP)

- **Chỉ cần làm 1 lần trên máy client**

1. Mở **Google Chrome** (hay bất kỳ trình duyệt Chrome-based)
2. Vào: `chrome://flags/#unsafely-treat-insecure-origin-as-secure` hoặc `[chrome-based_browser_name]://flags/#unsafely-treat-insecure-origin-as-secure`
3. **Bật** tùy chọn:  
   `Insecure origins treated as secure`
4. **Thêm địa chỉ server** vào danh sách: `http://`, **server_ip**, và `:8080` (Ví dụ: `http://10.15.193.245:8080`)
5. Nhấn **Relaunch** để khởi động lại Chrome

> Sau khi bật, WebView sẽ **cho phép dùng camera & micro** dù kết nối HTTP (không có HTTPS)

## Khi gọi thoại/video (sau khi bật flag)

1. Nhấn nút **Gọi** trong ứng dụng
2. **Popup yêu cầu quyền** sẽ hiện:
> **"Trang này muốn truy cập camera và micro"**
3. **Chọn**:
Allow this time
(hoặc **Allow** nếu có)

> **Lưu ý**:  
> - Nếu chọn **Block** → gọi sẽ **không có âm thanh/hình ảnh**  
> - Quyền chỉ cần cấp **1 lần** cho mỗi phiên làm việc  
> - Không cần cấp lại nếu đã **Allow this time**

---

**Kết quả**:  
- Gọi thoại/video hoạt động bình thường  
- Âm thanh & hình ảnh rõ nét 
- Không cần HTTPS nếu đã bật flag

---

## 🛠 Công cụ & Môi trường phát triển

| Thành phần | Công nghệ |
|----------|----------|
| **Backend** | Java 17+, Spring Boot 3.x, WebSocket, STOMP, Swagger |
| **Frontend** | Java Swing, Java-WebSocket, VLCj, |
| **Gọi thoại/video** | WebRTC + trang `call.html` |
| **Database** | MySQL |
| **Mã hóa** | RSA-2048, AES-256-GCM, PBKDF2, BCrypt |
| **Giao thức** | WebSocket, STOMP, WebRTC, HTTP/WSS |
| **IDE** | NetBeans / IntelliJ |

---

## ⚠️ Hạn chế hiện tại

| Vấn đề | Mô tả |
|-------|------|
| **Gửi/nhận file** | Khó khăn nếu client không cùng mạng với server (trừ khi nhận cùng máy với server) |
| **Kết nối server** | Phải **nhập IP thủ công** tại màn login hoặc sửa `NetworkService.java` |
| **Gọi thoại/video** | Trang gọi là **HTML → cần allow quyền micro/camera** <br> Nếu dùng **HTTP**: phải **Allow Insecure Content** |
| **Chat nhóm** | Chưa có E2EE |
| **Gọi nhóm** | Chưa triển khai |
| **Trạng thái online/offline** | Chưa triển khai |

---

## 📡 Kết nối LAN (Không cần Internet)

1. Chạy server trên máy chủ (VD IP: `192.168.x.x`)
2. Client nhập IP server tại **Login → Settings**
3. Kết nối thành công → chat, gọi, gửi file trong mạng nội bộ

> **Swagger UI**: `http://[IP]:8080/swagger-ui.html`  
> **Trang gọi mẫu**: `src/main/resources/static/call.html`

---

## 📄 Giấy phép

Dự án học tập – **LTM NetBean**  
Không sử dụng thương mại.

---

**SuperChat – Bảo mật từ client đến client, không ai can thiệp được.**  
*Gọi thoại/video qua WebRTC – chỉ cần cho phép quyền trong trình duyệt nhúng.*

---
