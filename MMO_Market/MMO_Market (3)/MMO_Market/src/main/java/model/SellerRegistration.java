package model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SellerRegistrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "shop_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String shopName;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column
    private String contract;

    @Column(name = "signed_contract")
    private String signedContract;

    @Column(length = 20)
    private String status = "Pending"; // Pending, Approved, Rejected

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
