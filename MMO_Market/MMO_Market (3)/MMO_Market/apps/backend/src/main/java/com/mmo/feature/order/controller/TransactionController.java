package com.mmo.feature.order.controller;
import com.mmo.shared.model.User;
import com.mmo.shared.dal.DigitalAssetRepository;
import com.mmo.shared.model.DigitalAsset;
import com.mmo.shared.model.Product;
import com.mmo.shared.dal.ReviewRepository;
import com.mmo.shared.dto.OrderDto;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Review;

import com.mmo.shared.dto.PurchaseRequest;
import com.mmo.shared.dto.PurchaseResponse;
import com.mmo.shared.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.order.service.TransactionService;

import java.util.Map;

import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.model.Complaint;

/**
 * Controller xử lý Giao dịch mua hàng (Purchase) dành cho Khách hàng (Customer).
 * Cung cấp API mua sản phẩm số, xem lịch sử đơn hàng đã mua và xem chi tiết một đơn hàng.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private com.mmo.shared.dal.UserRepository userRepository;

    @Autowired
    private com.mmo.shared.dal.ReviewRepository reviewRepository;

    @Autowired
    private com.mmo.shared.dal.ComplaintRepository complaintRepository;

    @Autowired
    private DigitalAssetRepository digitalAssetRepository;

    @Autowired
    private com.mmo.shared.utils.EncryptionUtils encryptionUtils;

    /**
     * API thực hiện thanh toán mua sản phẩm.
     * Trừ tiền trong ví khách hàng và cộng tiền vào ví tạm giữ (Escrow) của người bán.
     * Trả về thông tin đơn hàng cùng với tài khoản/mã kích hoạt số (Credentials).
     * @param request Chứa ID sản phẩm, Biến thể và Số lượng mua.
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseProduct(
            @AuthenticationPrincipal Long userId,
            @RequestBody PurchaseRequest request) {

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập trước khi thực hiện mua hàng."));
        }

        com.mmo.shared.model.User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() != null && (user.getRole().contains("Staff") || user.getRole().contains("Admin"))) {
            return ResponseEntity.status(403).body(Map.of("message", "Nhân viên và quản trị viên không được phép mua hàng."));
        }

        if (request.getProductId() == null || request.getVariantLabel() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu yêu cầu không hợp lệ."));
        }

        try {
            // Thực hiện transaction bọc trong service layer
            Transaction transaction = transactionService.purchaseProduct(userId, request.getProductId(), request.getVariantLabel(), request.getQuantity());

            // Lấy credentials thật từ DB (nếu có gán DigitalAsset)
            java.util.List<DigitalAsset> assignedAssets = digitalAssetRepository.findByTransactionAndIsDeleteFalse(transaction);
            
            PurchaseResponse.CredentialsDTO credentialsDTO = null;
            if (!assignedAssets.isEmpty()) {
                // Return the first credential in the response for backward compatibility,
                // or just leave it null and frontend can call detail endpoint to see all.
                // We will return the first one here.
                DigitalAsset asset = assignedAssets.get(0);
                if ("KEY".equalsIgnoreCase(asset.getAssetType()) || "GAME_CARD".equalsIgnoreCase(asset.getAssetType())) {
                    credentialsDTO = PurchaseResponse.CredentialsDTO.builder()
                            .username(asset.getKeyCode() != null ? encryptionUtils.decrypt(asset.getKeyCode()) : asset.getCardCode())
                            .password("(Product Key)")
                            .note(asset.getNotes())
                            .build();
                } else {
                    credentialsDTO = PurchaseResponse.CredentialsDTO.builder()
                            .username(asset.getAccountUsername())
                            .password(encryptionUtils.decrypt(asset.getAccountPassword()))
                            .note(asset.getNotes())
                            .build();
                }
            }

            // Sinh mã giao dịch và mã đơn hàng (khớp với DTO và API)
            String transactionCode = "TXN" + (10000000 + (long) (Math.random() * 90000000));
            String orderCode = "MMO-ORD-" + transaction.getId();

            PurchaseResponse response = PurchaseResponse.builder()
                    .transactionCode(transactionCode)
                    .orderCode(orderCode)
                    .finalBalance(transaction.getCustomer().getBalanceVnd())
                    .productName(transaction.getProduct() != null ? transaction.getProduct().getName() : "")
                    .amount(transaction.getAmountVnd())
                    .credentials(credentialsDTO)
                    .transactionId(transaction.getId())
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống khi thực hiện thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách các đơn hàng (lịch sử mua hàng) của Khách hàng hiện tại.
     * @return Danh sách OrderDto chứa thông tin tổng quan các đơn hàng.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyOrders(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập để xem đơn hàng."));
        }

        try {
            java.util.List<Transaction> transactions = transactionService.getMyOrders(userId);
            java.util.List<com.mmo.shared.dto.OrderDto> orders = transactions.stream().map(t -> {
                String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "PENDING";
                String paymentStatus = "PAID";
                if ("REFUNDED".equals(status)) {
                    paymentStatus = "REFUNDED";
                } else if ("CANCELLED".equals(status)) {
                    paymentStatus = "FAILED";
                }
                
                java.util.Optional<com.mmo.shared.model.Review> reviewOpt = reviewRepository.findByTransactionIdAndIsDeleteFalse(t.getId());
                boolean isReviewed = reviewOpt.isPresent();
                
                Long complaintId = complaintRepository.findFirstByTransactionIdAndIsDeleteFalseOrderByIdDesc(t.getId())
                        .map(Complaint::getId)
                        .orElse(null);

                return com.mmo.shared.dto.OrderDto.builder()
                        .orderCode("MMO-ORD-" + t.getId())
                        .transactionId(t.getId())
                        .productId(t.getProduct() != null ? t.getProduct().getId() : 0L)
                        .productName(t.getProduct() != null ? t.getProduct().getName() : "Sản phẩm đã xóa")
                        .variantLabel(t.getVariant() != null ? t.getVariant().getVariantName() : "")
                        .sellerName(t.getSeller() != null ? t.getSeller().getFullName() : "Người bán")
                        .sellerId(t.getSeller() != null ? t.getSeller().getId() : null)
                        .amount(t.getAmountVnd())
                        .quantity(t.getQuantity())
                        .status(status)
                        .paymentStatus(paymentStatus)
                        .createdAt(t.getCreatedAt() != null ? java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy").format(t.getCreatedAt()) : "")
                        .escrowReleaseDate(t.getEscrowReleaseDate() != null ? java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(t.getEscrowReleaseDate()) : "")
                        .isReviewed(isReviewed)
                        .reviewRating(isReviewed ? reviewOpt.get().getRating() : null)
                        .reviewComment(isReviewed ? reviewOpt.get().getComment() : null)
                        .complaintId(complaintId)
                        .build();
            }).toList();

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Xem chi tiết một đơn hàng đã mua (bao gồm cả tài khoản/mã kích hoạt số).
     * @param id ID của đơn hàng (Transaction ID).
     * @return Dữ liệu chi tiết đơn hàng (OrderDto).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionDetail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập để xem đơn hàng."));
        }

        try {
            Transaction t = transactionService.getTransactionDetail(id, userId);
            
            String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "PENDING";
            String paymentStatus = "PAID";
            if ("REFUNDED".equals(status)) {
                paymentStatus = "REFUNDED";
            } else if ("CANCELLED".equals(status)) {
                paymentStatus = "FAILED";
            }

            java.util.Optional<com.mmo.shared.model.Review> reviewOpt = reviewRepository.findByTransactionIdAndIsDeleteFalse(t.getId());
            boolean isReviewed = reviewOpt.isPresent();

            Long complaintId = complaintRepository.findFirstByTransactionIdAndIsDeleteFalseOrderByIdDesc(t.getId())
                    .map(Complaint::getId)
                    .orElse(null);

            com.mmo.shared.dto.OrderDto orderDto = com.mmo.shared.dto.OrderDto.builder()
                    .orderCode("MMO-ORD-" + t.getId())
                    .transactionId(t.getId())
                    .productId(t.getProduct() != null ? t.getProduct().getId() : 0L)
                    .productName(t.getProduct() != null ? t.getProduct().getName() : "Sản phẩm đã xóa")
                    .variantLabel(t.getVariant() != null ? t.getVariant().getVariantName() : "")
                    .sellerName(t.getSeller() != null ? t.getSeller().getFullName() : "Người bán")
                    .sellerId(t.getSeller() != null ? t.getSeller().getId() : null)
                    .amount(t.getAmountVnd())
                    .quantity(t.getQuantity())
                    .status(status)
                    .paymentStatus(paymentStatus)
                    .createdAt(t.getCreatedAt() != null ? java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy").format(t.getCreatedAt()) : "")
                    .escrowReleaseDate(t.getEscrowReleaseDate() != null ? java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(t.getEscrowReleaseDate()) : "")
                    .isReviewed(isReviewed)
                    .reviewRating(isReviewed ? reviewOpt.get().getRating() : null)
                    .reviewComment(isReviewed ? reviewOpt.get().getComment() : null)
                    .complaintId(complaintId)
                    .build();

            // Lấy thông tin tài sản số (nếu có)
            java.util.List<com.mmo.shared.model.DigitalAsset> assignedAssets = digitalAssetRepository.findByTransactionAndIsDeleteFalse(t);
            if (!assignedAssets.isEmpty()) {
                java.util.List<java.util.Map<String, String>> credsList = new java.util.ArrayList<>();
                for (DigitalAsset asset : assignedAssets) {
                    java.util.Map<String, String> creds = new java.util.HashMap<>();
                    if ("KEY".equalsIgnoreCase(asset.getAssetType()) || "GAME_CARD".equalsIgnoreCase(asset.getAssetType())) {
                        creds.put("username", asset.getKeyCode() != null ? encryptionUtils.decrypt(asset.getKeyCode()) : asset.getCardCode());
                        creds.put("password", "(Product Key)");
                    } else {
                        creds.put("username", asset.getAccountUsername());
                        creds.put("password", encryptionUtils.decrypt(asset.getAccountPassword()));
                    }
                    if (asset.getNotes() != null && !asset.getNotes().trim().isEmpty()) {
                        creds.put("note", asset.getNotes());
                    }
                    credsList.add(creds);
                }
                orderDto.setCredentialsList(credsList);
                if (credsList.size() > 0) {
                    orderDto.setCredentials(credsList.get(0)); // backward compatibility
                }
            }

            return ResponseEntity.ok(orderDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi khi lấy chi tiết đơn hàng: " + e.getMessage()));
        }
    }
}
