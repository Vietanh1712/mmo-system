package com.mmo.shared.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "Transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "commission_vnd", nullable = false)
    private Long commissionVnd;

    @Column(length = 20)
    @Builder.Default
    private String status = "Pending"; // Pending, Held, Completed, Refunded, Cancelled, Disputed

    @Column(name = "escrow_release_date")
    private LocalDateTime escrowReleaseDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

    @Column(name = "payment_method")
    private String paymentMethod;

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
