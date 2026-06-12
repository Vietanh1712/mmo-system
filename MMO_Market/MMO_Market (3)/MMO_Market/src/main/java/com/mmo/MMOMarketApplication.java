package com.mmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {
        "controller",
        "service",
        "dal",
        "security",
        "config"
})
@EnableAsync
public class MMOMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(MMOMarketApplication.class, args);
        System.out.println("========================================");
        System.out.println("✅ MMO Market System is running!");
        System.out.println("📍 API Base URL: http://localhost:8080");
        System.out.println("📖 API Docs: http://localhost:8080/api/auth/health");
        System.out.println("========================================");
    }
}