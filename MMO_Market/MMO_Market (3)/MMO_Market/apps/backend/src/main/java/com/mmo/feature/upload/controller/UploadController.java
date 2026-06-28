package com.mmo.feature.upload.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập trước khi tải lên tệp tin."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tệp tin tải lên không được để trống."));
        }

        try {
            // Create uploads directory if not exists
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Sanitize filename to prevent directory traversal and special chars issues
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Limit files to common image/video extensions for security
            String extLower = extension.toLowerCase();
            if (!extLower.equals(".png") && !extLower.equals(".jpg") && !extLower.equals(".jpeg") && 
                !extLower.equals(".gif") && !extLower.equals(".mp4") && !extLower.equals(".webm") && 
                !extLower.equals(".avi") && !extLower.equals(".mov")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Hệ thống chỉ hỗ trợ các định dạng ảnh và video thông dụng."));
            }

            String sanitizedBase = System.currentTimeMillis() + "_" + Math.floor(Math.random() * 10000);
            String fileName = sanitizedBase + extension;

            Path path = Paths.get(UPLOAD_DIR, fileName);
            Files.copy(file.getInputStream(), path);

            String fileUrl = "/uploads/" + fileName;
            return ResponseEntity.ok(Map.of("url", fileUrl));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lưu tệp tin: " + e.getMessage()));
        }
    }
}
