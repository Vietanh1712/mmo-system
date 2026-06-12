package model;

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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "sepay_code", length = 255)
    private String sepayCode;

    @Column(length = 20)
    private String status; // Pending, Success, Failed

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    private Boolean isDelete;

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
