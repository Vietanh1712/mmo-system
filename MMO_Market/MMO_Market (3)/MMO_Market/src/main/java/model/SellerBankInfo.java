package model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SellerBankInfo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerBankInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String branch;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    private Boolean isDelete = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDelete == null) {
            isDelete = false;
        }
    }
}
