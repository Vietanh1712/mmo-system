package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(nullable = false)
    private Integer rating; // 1 to 5

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "media_url", columnDefinition = "NVARCHAR(MAX)")
    private String mediaUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete", nullable = false)
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
    }
}
