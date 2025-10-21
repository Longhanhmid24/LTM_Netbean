package com.app.chatserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class ChatserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatserverApplication.class, args);
    }
}