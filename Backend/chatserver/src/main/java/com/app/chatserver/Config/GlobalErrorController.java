package com.app.chatserver.Config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusCode = request.getAttribute("jakarta.servlet.error.status_code");

        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusCode != null) {
            status = (int) statusCode;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        body.put("message", "Có lỗi xảy ra khi xử lý yêu cầu.");
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, HttpStatus.valueOf(status));
    }
}
