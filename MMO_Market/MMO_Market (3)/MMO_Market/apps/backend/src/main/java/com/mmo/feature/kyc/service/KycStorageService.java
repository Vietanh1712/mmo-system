package com.mmo.feature.kyc.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class KycStorageService {

    @Value("${mmo.kyc.storage-path:${user.home}/.mmo/kyc}")
    private String storagePathStr;

    private Path storagePath;

    @PostConstruct
    public void init() throws IOException {
        storagePath = Paths.get(storagePathStr).toAbsolutePath().normalize();
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng.");
        }
        
        // Basic Magic Byte Validation (JPEG, PNG, WEBP)
        byte[] magicBytes = new byte[12];
        try (var in = file.getInputStream()) {
            in.read(magicBytes);
        }
        String ext = detectExtension(magicBytes);
        if (ext == null) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Chỉ nhận JPEG, PNG, WEBP.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước file không được vượt quá 5MB.");
        }

        String fileName = UUID.randomUUID().toString() + ext;
        Path targetLocation = storagePath.resolve(fileName);
        
        Files.copy(file.getInputStream(), targetLocation);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            Files.deleteIfExists(targetLocation);
                            log.info("Rollback: Đã xoá file {}", fileName);
                        } catch (IOException e) {
                            log.error("Lỗi khi xoá file sau khi rollback: {}", fileName, e);
                        }
                    }
                }
            });
        }

        return fileName;
    }

    public File getFile(String fileName) {
        Path filePath = storagePath.resolve(fileName).normalize();
        // Check directory traversal
        if (!filePath.startsWith(storagePath)) {
            return null;
        }
        File file = filePath.toFile();
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private String detectExtension(byte[] bytes) {
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return ".jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 && 
            (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) {
            return ".png";
        }
        if (bytes.length >= 12 && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 && 
            (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46 &&
            (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45 && 
            (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) {
            return ".webp";
        }
        return null;
    }
}
