# Base image: OpenJDK 17 (phiên bản ổn định và nhẹ)
FROM openjdk:17-jdk-slim

# Đặt thư mục làm việc trong container
WORKDIR /app

# Copy file JAR đã build từ thư mục target vào container
# (đường dẫn có thể chỉnh lại nếu cần)
COPY ../target/*.jar app.jar

# Mở cổng 8080 cho Spring Boot
EXPOSE 8080

# Lệnh khởi động ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
