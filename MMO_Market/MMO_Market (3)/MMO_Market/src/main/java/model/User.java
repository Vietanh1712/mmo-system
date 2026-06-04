package model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column
    private String gender;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String role; // JSON format: {"role": "Customer"} or {"role": "Seller"}

    @Column
    private String phone;

    @Column(name = "shop_status")
    private String shopStatus; // Pending, Active, Banned

    @Column(name = "balance_vnd")
    private Long balanceVnd; // Default: 0

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String permissions;

    @Column(name = "isVerified")
    private Boolean isVerified; // Default: false

    @Column(name = "isLocked")
    private Boolean isLocked; // Default: false

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "isDelete")
    private Boolean isDelete; // Default: false (soft delete)

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (balanceVnd == null) {
            balanceVnd = 0L;
        }
        if (isVerified == null) {
            isVerified = false;
        }
        if (isLocked == null) {
            isLocked = false;
        }
        if (isDelete == null) {
            isDelete = false;
        }
        if (shopStatus == null) {
            shopStatus = "Pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
