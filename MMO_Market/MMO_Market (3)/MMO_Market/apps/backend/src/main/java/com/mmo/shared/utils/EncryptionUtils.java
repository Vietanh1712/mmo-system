package com.mmo.shared.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncryptionUtils {

    private static EncryptionUtils instance;

    private final SecretKeySpec secretKey;

    public EncryptionUtils(@Value("${app.encryption.secret:MySecretKeyForDigitalAssets2026!}") String secret) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] finalKey = new byte[32];
            System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 32));
            this.secretKey = new SecretKeySpec(finalKey, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptionUtils key spec", e);
        }
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static EncryptionUtils getInstance() {
        return instance;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during encryption: " + e.getMessage(), e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback for plain text data
            return cipherText;
        }
    }
}
