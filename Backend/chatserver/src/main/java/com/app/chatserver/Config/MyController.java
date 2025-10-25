package com.app.chatserver.Config;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth") // Apply the defined security scheme
public class MyController {

    @GetMapping("/secured-endpoint")
    public String getSecuredData() {
        return "This is secured data!";
    }
}