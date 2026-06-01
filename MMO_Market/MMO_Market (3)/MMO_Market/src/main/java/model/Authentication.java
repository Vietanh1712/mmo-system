package model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Authentications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Authentication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String provider; // 'System' or 'Google'

    @Column(name = "third_party_token")
    private String thirdPartyToken;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "refresh_token_expiry_date")
    private LocalDateTime refreshTokenExpiryDate;

    @Column(name = "is_revoked")
    private Boolean isRevoked;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    private Boolean isDelete; // Default: false

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDelete == null) {
            isDelete = false;
        }
        if (isRevoked == null) {
            isRevoked = false;
        }
    }
}