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
            // ✅ Tắt CSRF cho REST API
            .csrf(csrf -> csrf.disable())
            
            // ✅ Cho phép Swagger và API public
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/ws/**", 
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/api/**",
                    "/error" // tránh lỗi circular view khi error 403
                ).permitAll()
                .anyRequest().permitAll()
            )
            
            // ✅ Tắt form login & basic auth mặc định (ngăn 403)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
