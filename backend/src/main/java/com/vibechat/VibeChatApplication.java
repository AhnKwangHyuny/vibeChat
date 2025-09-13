package com.vibechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
//@EnableScheduling // 스케줄링 기능 활성화
public class VibeChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeChatApplication.class, args);
    }

}
