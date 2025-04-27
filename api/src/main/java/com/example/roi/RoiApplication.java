package com.example.roi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class RoiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoiApplication.class, args);
    }
}
