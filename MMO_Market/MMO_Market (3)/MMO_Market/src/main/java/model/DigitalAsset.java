package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DigitalAssets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigitalAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    /**
     * Loại tài sản: ACCOUNT | KEY | GAME_CARD
     */
    @Column(name = "asset_type", nullable = false, length = 20)
    private String assetType;

    /**
     * Dữ liệu JSON:
     *  - ACCOUNT: {"username":"...","password":"...","note":"..."}
     *  - KEY/GAME_CARD: {"key":"...","note":"..."}
     */
    @Column(name = "asset_data", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String assetData;

    /**
     * Tài khoản username (cho loại ACCOUNT)
     */
    @Column(name = "account_username", length = 255)
    private String accountUsername;

    /**
     * Tài khoản password (cho loại ACCOUNT)
     */
    @Column(name = "account_password", columnDefinition = "NVARCHAR(500)")
    private String accountPassword;

    /**
     * Mã key/code (cho loại KEY)
     */
    @Column(name = "key_code", columnDefinition = "NVARCHAR(MAX)")
    private String keyCode;

    /**
     * Mã code thẻ game (cho loại GAME_CARD)
     */
    @Column(name = "card_code", columnDefinition = "NVARCHAR(MAX)")
    private String cardCode;

    /**
     * PIN thẻ game (cho loại GAME_CARD)
     */
    @Column(name = "card_pin", length = 255)
    private String cardPin;

    /**
     * Ghi chú chung
     */
    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    /**
     * false = còn hàng, true = đã bán / đã giao khách
     */
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "is_delete", nullable = false)
    private Boolean isDelete = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
