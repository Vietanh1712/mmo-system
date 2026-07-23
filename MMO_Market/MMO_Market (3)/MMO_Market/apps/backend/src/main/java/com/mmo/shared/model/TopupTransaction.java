package com.mmo.shared.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "TopupTransactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopupTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "amount_vnd")
    private Long amountVnd;

    @Column(name = "sepay_code", length = 255)
    private String sepayCode;

    @Column(length = 20)
    private String status; // Pending, Success, Failed

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    private Boolean isDelete;

    @Column(name = "transfer_content", length = 500)
    private String transferContent;

    @Column(name = "balance_before")
    private Long balanceBefore;

    @Column(name = "balance_after")
    private Long balanceAfter;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "staff_note", length = 500)
    private String staffNote;

    @Column(name = "processed_by_staff_id")
    private Long processedByStaffId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDelete == null) {
            isDelete = false;
        }
        if (status == null) {
            status = "Pending";
        }
    }
}
