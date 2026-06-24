package model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "KYCRequests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "citizen_id")
    private String citizenId;

    @Column(name = "front_id_image")
    private String frontIdImage;

    @Column(name = "back_id_image")
    private String backIdImage;

    @Column(name = "selfie_image")
    private String selfieImage;

    @Column(name = "status")
    private String status;

    @Column(name = "isDelete")
    private Boolean isDelete;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "type_kyc")
    private String typeKyc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            insertable = false,
            updatable = false
    )
    private User user;
}