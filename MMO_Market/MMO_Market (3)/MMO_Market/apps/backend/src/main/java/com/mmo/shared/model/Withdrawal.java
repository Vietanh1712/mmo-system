package com.mmo.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Withdrawals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Withdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_info_id", nullable = false)
    private SellerBankInfo bankInfo;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(length = 20)
    private String status = "Pending"; // Pending, Completed, Failed

    @Column(name = "proof_file")
    private String proofFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionReason;

    @Column(name = "fee_vnd")
    private Long feeVnd = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    private Boolean isDelete = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "Pending";
        }
        if (isDelete == null) {
            isDelete = false;
        }
    }
}
