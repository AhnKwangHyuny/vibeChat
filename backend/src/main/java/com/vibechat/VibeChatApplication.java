package com.vibechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VibeChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeChatApplication.class, args);
    }

}
