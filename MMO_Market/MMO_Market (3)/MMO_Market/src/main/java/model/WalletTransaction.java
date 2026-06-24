package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "WalletTransactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50, nullable = false)
    private String type; // TOPUP, PAYMENT, REFUND, ESCROW, WITHDRAWAL

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(length = 20, nullable = false)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(length = 255)
    private String description;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "transaction_type", length = 50, nullable = false)
    private String transactionType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    @Builder.Default
    private Boolean isDelete = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDelete == null) {
            isDelete = false;
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
