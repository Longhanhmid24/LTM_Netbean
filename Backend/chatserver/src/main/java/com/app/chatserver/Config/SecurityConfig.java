package com.app.chatserver.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ✅ Tắt CSRF cho API/WebSocket
            .csrf(csrf -> csrf.disable())

            // ✅ Cho phép tất cả endpoint public (kể cả uploads, error)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/ws/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/api/**",
                    "/uploads/**",   // 🟢 mở quyền truy cập static file
                    "/error"         // 🟢 tránh vòng lặp lỗi error
                ).permitAll()
                .anyRequest().permitAll()
            )

            // ✅ Tắt form login & basic auth mặc định
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // ✅ Xử lý lỗi trả về dạng JSON thay vì view
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\": \"Access Denied\"}");
                })
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\": \"Unauthorized\"}");
                })
            );

        return http.build();
    }
}
