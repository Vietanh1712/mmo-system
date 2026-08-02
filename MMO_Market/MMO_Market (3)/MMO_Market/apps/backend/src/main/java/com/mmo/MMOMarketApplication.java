package com.mmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MMOMarketApplication {

    static {
        try {
            java.io.File envFile = null;
            String[] pathsToCheck = {
                ".env",
                "apps/backend/.env",
                "../.env",
                "../../.env"
            };
            for (String path : pathsToCheck) {
                java.io.File file = new java.io.File(path);
                if (file.exists() && file.isFile()) {
                    envFile = file;
                    break;
                }
            }
            if (envFile != null) {
                java.nio.file.Files.lines(envFile.toPath())
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    });
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MMOMarketApplication.class, args);
        System.out.println("========================================");
        System.out.println("✅ MMO Market System is running!");
        System.out.println("📍 API Base URL: http://localhost:8080");
        System.out.println("📖 API Docs: http://localhost:8080/api/auth/health");
        System.out.println("========================================");
    }
}
