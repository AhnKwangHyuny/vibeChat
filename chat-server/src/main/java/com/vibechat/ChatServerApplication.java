package com.vibechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableCaching
@EnableMongoRepositories(basePackages = "com.vibechat.repository")
public class ChatServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServerApplication.class, args);
    }
}
