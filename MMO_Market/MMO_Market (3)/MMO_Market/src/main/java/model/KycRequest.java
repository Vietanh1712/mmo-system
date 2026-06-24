package model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "KYCRequests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "active_user_id")
    private Long activeUserId;

    @Column(name = "id_number", length = 50, nullable = false)
    private String idNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", length = 50, nullable = false)
    private IdType idType;

    @Column(name = "request_code", length = 32, nullable = false, unique = true)
    private String requestCode;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "front_id_image", length = 255)
    private String frontIdImage;

    @Column(name = "back_id_image", length = 255)
    private String backIdImage;

    @Column(name = "selfie_image", length = 255)
    private String selfieImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private KycStatus status; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(500)")
    private String rejectionReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "isDelete", nullable = false)
    private Boolean isDelete; // Default: false

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = KycStatus.PENDING;
        }
        if (isDelete == null) {
            isDelete = false;
        }
        if (version == null) {
            version = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
