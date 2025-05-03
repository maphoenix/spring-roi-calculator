package com.example.roi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Allow requests from the typical Vite/React dev server origins
                registry.addMapping("/api/**") // Configure CORS for paths under /api
                        .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000") // Add other origins if needed
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow common methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(false); // Adjust if credentials (cookies) are needed
            }
        };
    }
}
