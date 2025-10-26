package com.app.chatserver.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat Server API")
                        .description("API documentation for the ChatServer project")
                        .version("1.0"));
    }
}
