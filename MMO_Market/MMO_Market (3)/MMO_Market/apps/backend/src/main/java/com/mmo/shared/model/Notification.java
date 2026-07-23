package com.mmo.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(length = 50, nullable = false)
    private String type; // info, warning, maintenance, policy, SYSTEM, ORDER, WALLET, KYC, SECURITY, COMPLAINT

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "isDelete")
    private Boolean isDelete; // soft delete flag

    @Column(name = "isRead")
    private Boolean isRead; // read status for user's personal notifications

    @Column(length = 50)
    private String severity; // INFO, WARNING, DANGER, SUCCESS

    @Column(name = "target_url", length = 500)
    private String targetUrl; // URL link for user redirection

    @Column(length = 20)
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isDelete == null) {
            isDelete = false;
        }
        if (isRead == null) {
            isRead = false;
        }
        if (type == null) {
            type = "info";
        }
        if (severity == null) {
            severity = "INFO";
        }
        if (status == null) {
            status = "PUBLISHED";
        }
    }
}
