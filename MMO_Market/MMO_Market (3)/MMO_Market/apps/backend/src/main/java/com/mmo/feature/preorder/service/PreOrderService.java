package com.mmo.feature.preorder.service;
import com.mmo.shared.model.Transaction;

import com.mmo.shared.dto.PreOrderRequest;
import com.mmo.shared.dto.PreOrderResponse;
import com.mmo.shared.dal.PreOrderRepository;
import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.PreOrder;
import com.mmo.shared.model.Product;
import com.mmo.shared.model.ProductVariant;
import com.mmo.shared.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.mmo.shared.dal.ProductVariantRepository;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.model.Notification;

@Service
@Slf4j
public class PreOrderService {

    private final PreOrderRepository preOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final com.mmo.shared.dal.DigitalAssetRepository digitalAssetRepository;
    private final NotificationRepository notificationRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.mmo.shared.dal.TransactionRepository transactionRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.mmo.shared.dal.ComplaintRepository complaintRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.mmo.feature.wallet.service.WalletService walletService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.mmo.shared.dal.SystemConfigurationRepository systemConfigurationRepository;

    public PreOrderService(PreOrderRepository preOrderRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository,
                           com.mmo.shared.dal.DigitalAssetRepository digitalAssetRepository,
                           NotificationRepository notificationRepository) {
        this.preOrderRepository = preOrderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.digitalAssetRepository = digitalAssetRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public PreOrderResponse createPreOrder(Long customerId, PreOrderRequest request) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đặt trước không hợp lệ.");
        }
        if (request.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không được để trống.");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0.");
        }
        if (request.getExpectedPriceVnd() == null || request.getExpectedPriceVnd() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng giá đặt trước phải lớn hơn 0.");
        }

        User customer = userRepository.findByIdAndIsDeleteFalse(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ."));

        Product product = productRepository.findByIdAndIsDeleteFalse(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));

        ProductVariant variant = resolveRequestedVariant(product, request.getVariantId());

        // Kiểm tra số dư ví của khách hàng
        long expectedPrice = request.getExpectedPriceVnd();
        long currentBalance = customer.getBalanceVnd() != null ? customer.getBalanceVnd() : 0L;
        if (currentBalance < expectedPrice) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư ví không đủ để thực hiện đặt trước.");
        }

        // Khấu trừ tiền ví khách hàng
        long newBalance = currentBalance - expectedPrice;
        customer.setBalanceVnd(newBalance);
        userRepository.save(customer);

        PreOrder preOrder = new PreOrder();
        preOrder.setCustomer(customer);
        preOrder.setProduct(product);
        preOrder.setVariant(variant);
        preOrder.setQuantity(request.getQuantity());
        preOrder.setExpectedPriceVnd(request.getExpectedPriceVnd());
        preOrder.setNotes(request.getNotes() == null ? null : request.getNotes().trim());

        PreOrder saved = preOrderRepository.save(preOrder);

        // Ghi log giao dịch ví đóng băng cho đơn Pre-order
        try {
            walletService.recordTransaction(
                    customer,
                    "PREORDER",
                    -expectedPrice,
                    "SUCCESS",
                    "Giam tiền đặt trước sản phẩm " + product.getName() + " (Đơn PO-" + saved.getId() + ")",
                    "PO-HOLD-" + saved.getId(),
                    newBalance,
                    saved.getId()
            );
        } catch (Exception e) {
            log.error("Lỗi ghi giao dịch ví PreOrder: {}", e.getMessage());
        }

        if (product.getSeller() != null) {
            try {
                Notification sellerNotification = Notification.builder()
                        .userId(product.getSeller().getId())
                        .title("Bạn có đơn đặt trước mới")
                        .content("Đơn PO-" + saved.getId()
                                + " đặt " + saved.getQuantity() + " tài khoản của sản phẩm '"
                                + product.getName() + "'"
                                + (variant != null ? ", biến thể '" + variant.getVariantName() + "'" : "")
                                + ".")
                        .type("ORDER")
                        .severity("INFO")
                        .isRead(false)
                        .isDelete(false)
                        .createdAt(java.time.LocalDateTime.now())
                        .targetUrl("/seller/console")
                        .build();
                notificationRepository.save(sellerNotification);
            } catch (Exception e) {
                log.warn("Lỗi gửi thông báo cho seller: {}", e.getMessage());
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return PreOrderResponse.builder()
                .success(true)
                .message("Đã gửi yêu cầu đặt trước thành công.")
                .id(saved.getId())
                .productId(product.getId())
                .productName(product.getName())
                .variantId(variant != null ? variant.getId() : null)
                .variantName(variant != null ? variant.getVariantName() : null)
                .quantity(saved.getQuantity())
                .expectedPriceVnd(saved.getExpectedPriceVnd())
                .status(saved.getStatus())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().format(formatter) : "")
                .build();
    }

    @Transactional(readOnly = true)
    public List<PreOrderResponse> getPreOrdersByCustomer(Long customerId) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        User customer = userRepository.findByIdAndIsDeleteFalse(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ."));

        List<PreOrder> preOrders = preOrderRepository.findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return preOrders.stream()
                .map(po -> PreOrderResponse.builder()
                        .success(true)
                        .id(po.getId())
                        .productId(po.getProduct().getId())
                        .productName(po.getProduct().getName())
                        .variantId(po.getVariant() != null ? po.getVariant().getId() : null)
                        .variantName(po.getVariant() != null ? po.getVariant().getVariantName() : null)
                        .quantity(po.getQuantity())
                        .expectedPriceVnd(po.getExpectedPriceVnd())
                        .status(po.getStatus())
                        .notes(po.getNotes())
                        .deliveryData(po.getDeliveryData())
                        .createdAt(po.getCreatedAt() != null ? po.getCreatedAt().format(formatter) : "")
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PreOrderResponse> getPreOrdersBySeller(Long sellerId) {
        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        User seller = userRepository.findByIdAndIsDeleteFalse(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ."));

        List<PreOrder> preOrders = preOrderRepository.findBySellerOrderByCreatedAtDesc(seller);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return preOrders.stream()
                .map(po -> PreOrderResponse.builder()
                        .success(true)
                        .id(po.getId())
                        .productId(po.getProduct().getId())
                        .productName(po.getProduct().getName())
                        .variantId(po.getVariant() != null ? po.getVariant().getId() : null)
                        .variantName(po.getVariant() != null ? po.getVariant().getVariantName() : null)
                        .customerEmail(po.getCustomer().getEmail())
                        .quantity(po.getQuantity())
                        .expectedPriceVnd(po.getExpectedPriceVnd())
                        .status(po.getStatus())
                        .notes(po.getNotes())
                        .deliveryData(po.getDeliveryData())
                        .createdAt(po.getCreatedAt() != null ? po.getCreatedAt().format(formatter) : "")
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public PreOrderResponse updatePreOrderStatus(Long sellerId, Long preOrderId, String newStatus) {
        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        PreOrder preOrder = preOrderRepository.findById(preOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt trước."));

        if (!preOrder.getProduct().getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật đơn này.");
        }

        // Nếu chuyển trạng thái sang CANCELLED hoặc REJECTED, thực hiện hoàn tiền cho khách hàng
        boolean isCancellation = "CANCELLED".equalsIgnoreCase(newStatus) || "REJECTED".equalsIgnoreCase(newStatus) || "Bị từ chối".equalsIgnoreCase(newStatus) || "Đã hủy".equalsIgnoreCase(newStatus);
        
        if (isCancellation && !"CANCELLED".equalsIgnoreCase(preOrder.getStatus()) && !"REJECTED".equalsIgnoreCase(preOrder.getStatus())) {
            User customer = preOrder.getCustomer();
            long refundAmount = preOrder.getExpectedPriceVnd();
            long oldBalance = customer.getBalanceVnd() != null ? customer.getBalanceVnd() : 0L;
            long newBalance = oldBalance + refundAmount;
            customer.setBalanceVnd(newBalance);
            userRepository.save(customer);

            try {
                walletService.recordTransaction(
                        customer,
                        "REFUND",
                        refundAmount,
                        "SUCCESS",
                        "Seller hủy/từ chối đơn đặt trước sản phẩm " + preOrder.getProduct().getName() + " (Đơn PO-" + preOrder.getId() + ")",
                        "PO-REFUND-" + preOrder.getId(),
                        newBalance,
                        preOrder.getId()
                );
            } catch (Exception e) {
                log.error("Lỗi ghi giao dịch hoàn tiền khi Seller hủy PreOrder: {}", e.getMessage());
            }
        }

        preOrder.setStatus(newStatus);
        PreOrder saved = preOrderRepository.save(preOrder);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return PreOrderResponse.builder()
                .success(true)
                .message("Cập nhật trạng thái thành công.")
                .id(saved.getId())
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getName())
                .variantId(saved.getVariant() != null ? saved.getVariant().getId() : null)
                .variantName(saved.getVariant() != null ? saved.getVariant().getVariantName() : null)
                .customerEmail(saved.getCustomer().getEmail())
                .quantity(saved.getQuantity())
                .expectedPriceVnd(saved.getExpectedPriceVnd())
                .status(saved.getStatus())
                .notes(saved.getNotes())
                .deliveryData(saved.getDeliveryData())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().format(formatter) : "")
                .build();
    }

    @Transactional
    public PreOrderResponse cancelPreOrder(Long customerId, Long preOrderId) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        PreOrder preOrder = preOrderRepository.findById(preOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt trước."));

        if (!preOrder.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy đơn này.");
        }

        String status = preOrder.getStatus();
        boolean canCancel = "PENDING".equalsIgnoreCase(status) 
                || "Chờ xử lý".equalsIgnoreCase(status);

        if (!canCancel) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy đơn đặt trước ở trạng thái chờ xử lý.");
        }

        // Cancel the pre-order status
        preOrder.setStatus("CANCELLED");
        PreOrder saved = preOrderRepository.save(preOrder);

        // Refund customer wallet balance
        User customer = preOrder.getCustomer();
        long refundAmount = preOrder.getExpectedPriceVnd();
        long oldBalance = customer.getBalanceVnd() != null ? customer.getBalanceVnd() : 0L;
        long newBalance = oldBalance + refundAmount;
        customer.setBalanceVnd(newBalance);
        userRepository.save(customer);

        // Record the refund wallet transaction
        try {
            walletService.recordTransaction(
                    customer,
                    "REFUND",
                    refundAmount,
                    "SUCCESS",
                    "Hoàn tiền hủy đơn đặt trước sản phẩm " + preOrder.getProduct().getName() + " (Đơn PO-" + saved.getId() + ")",
                    "PO-REFUND-" + saved.getId(),
                    newBalance,
                    saved.getId()
            );
        } catch (Exception e) {
            log.error("Lỗi ghi giao dịch hoàn tiền PreOrder: {}", e.getMessage());
        }

        // Thông báo cho Seller
        try {
            Product product = productRepository.findById(saved.getProduct().getId()).orElse(null);
            if (product != null && product.getSeller() != null) {
                Notification sellerNotification = Notification.builder()
                        .userId(product.getSeller().getId())
                        .title("Khách hàng đã hủy đơn đặt trước")
                        .content("Khách hàng " + saved.getCustomer().getEmail() 
                                + " đã hủy đơn đặt trước PO-" + saved.getId() 
                                + " cho sản phẩm '" + product.getName() + "'.")
                        .type("ORDER")
                        .severity("WARNING")
                        .isRead(false)
                        .isDelete(false)
                        .createdAt(java.time.LocalDateTime.now())
                        .targetUrl("/seller/console")
                        .build();
                notificationRepository.save(sellerNotification);
                log.info("Đã tạo thông báo hủy đơn Pre-Order cho seller ID: {}", product.getSeller().getId());
            } else {
                log.warn("Không thể gửi thông báo hủy đơn đặt trước: product hoặc seller bị null");
            }
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo hủy đơn cho seller: {}", e.getMessage(), e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return PreOrderResponse.builder()
                .success(true)
                .message("Hủy đơn đặt trước và hoàn tiền thành công.")
                .id(saved.getId())
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getName())
                .variantId(saved.getVariant() != null ? saved.getVariant().getId() : null)
                .variantName(saved.getVariant() != null ? saved.getVariant().getVariantName() : null)
                .quantity(saved.getQuantity())
                .expectedPriceVnd(saved.getExpectedPriceVnd())
                .status(saved.getStatus())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().format(formatter) : "")
                .build();
    }

    @Transactional
    public PreOrderResponse deliverPreOrder(Long sellerId, Long preOrderId, com.mmo.shared.dto.PreOrderDeliveryRequest request) {
        PreOrder preOrder = preOrderRepository.findById(preOrderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt trước"));

        if (!preOrder.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này");
        }

        if (!preOrder.getStatus().equalsIgnoreCase("PENDING") 
                && !preOrder.getStatus().equalsIgnoreCase("Chờ xử lý")
                && !preOrder.getStatus().equalsIgnoreCase("APPROVED")
                && !preOrder.getStatus().equalsIgnoreCase("Đã duyệt")) {
            throw new RuntimeException("Chỉ có thể giao hàng cho đơn đang chờ xử lý hoặc đã duyệt");
        }

        preOrder.setStatus("COMPLETED");
        preOrder.setDeliveryData(request.getDeliveryData());
        if (request.getProofImage() != null && !request.getProofImage().isBlank()) {
            preOrder.setProofImage(request.getProofImage());
        }
        
        PreOrder saved = preOrderRepository.save(preOrder);

        // Tạo giao dịch Escrow thực tế cho Seller
        try {
            createEscrowTransactionForPreOrder(saved);
        } catch (Exception e) {
            log.error("Lỗi tạo giao dịch Escrow khi giao hàng PreOrder thủ công: {}", e.getMessage());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return PreOrderResponse.builder()
                .id(saved.getId())
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getName())
                .variantId(saved.getVariant() != null ? saved.getVariant().getId() : null)
                .variantName(saved.getVariant() != null ? saved.getVariant().getVariantName() : null)
                .customerEmail(saved.getCustomer().getEmail())
                .quantity(saved.getQuantity())
                .expectedPriceVnd(saved.getExpectedPriceVnd())
                .status(saved.getStatus())
                .notes(saved.getNotes())
                .deliveryData(saved.getDeliveryData())
                .proofImage(saved.getProofImage())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().format(formatter) : "")
                .build();
    }

    @Transactional
    public void autoFulfillPreOrders(com.mmo.shared.model.ProductVariant variant, List<com.mmo.shared.model.DigitalAsset> newAssets) {
        if (variant == null) return;

        Product product = variant.getProduct();
        List<PreOrder> allPreOrders = preOrderRepository.findByProductAndIsDeleteFalseOrderByCreatedAtAsc(product);
        List<PreOrder> pendingPreOrders = allPreOrders.stream()
                .filter(po -> {
                    String s = po.getStatus();
                    return "PENDING".equalsIgnoreCase(s) 
                            || "Chờ xử lý".equalsIgnoreCase(s) 
                            || "APPROVED".equalsIgnoreCase(s) 
                            || "Đã duyệt".equalsIgnoreCase(s)
                            || "ACCEPTED".equalsIgnoreCase(s);
                })
                .collect(Collectors.toList());

        if (pendingPreOrders.isEmpty()) return;

        // New preorders are bound to a variant. Legacy rows without variant_id
        // remain eligible so existing data can still be fulfilled.
        boolean legacyVariantIsUnambiguous =
                productVariantRepository.findByProductAndIsDeleteFalse(product).size() == 1;
        pendingPreOrders = pendingPreOrders.stream()
                .filter(po -> {
                    if (po.getVariant() != null) {
                        return po.getVariant().getId().equals(variant.getId());
                    }
                    if (legacyVariantIsUnambiguous) {
                        po.setVariant(variant);
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (pendingPreOrders.isEmpty()) return;

        // Lock and inspect the complete available inventory, not only the latest
        // upload batch. This lets quantity=2 be fulfilled when accounts arrive
        // in two separate seller updates and prevents concurrent double-allocation.
        List<com.mmo.shared.model.DigitalAsset> availableAssets =
                new java.util.ArrayList<>(digitalAssetRepository.findAvailableForUpdate(variant));

        for (PreOrder preOrder : pendingPreOrders) {
            if (availableAssets.isEmpty()) break;

            int needed = preOrder.getQuantity() != null ? preOrder.getQuantity() : 1;
            if (availableAssets.size() < needed) {
                // FIFO: do not skip an older preorder just because a newer,
                // smaller order could be filled.
                break;
            }

            if (availableAssets.size() >= needed) {
                // Fulfill this preorder
                Transaction tx = null;
                try {
                    tx = createEscrowTransactionForPreOrder(preOrder);
                } catch (Exception e) {
                    log.error("Lỗi tạo giao dịch Escrow khi tự động giao hàng PreOrder: {}", e.getMessage());
                }

                List<String> deliveryDataList = new java.util.ArrayList<>();
                for (int i = 0; i < needed; i++) {
                    com.mmo.shared.model.DigitalAsset asset = availableAssets.remove(0);
                    asset.setIsUsed(true);
                    if (tx != null) {
                        asset.setTransaction(tx);
                    }
                    digitalAssetRepository.save(asset);

                    String data = "Tài khoản " + (i + 1) + "\n";
                    if (asset.getAssetType().equalsIgnoreCase("ACCOUNT")) {
                        data += "Tên đăng nhập: " + asset.getAccountUsername() + "\nMật khẩu: " + asset.getAccountPassword();
                    } else if (asset.getAssetType().equalsIgnoreCase("KEY")) {
                        data += "Mã Key: " + asset.getKeyCode();
                    } else if (asset.getAssetType().equalsIgnoreCase("CARD")
                            || asset.getAssetType().equalsIgnoreCase("GAME_CARD")) {
                        data += "Mã thẻ: " + asset.getCardCode() + "\nMã PIN/Seri: " + asset.getCardPin();
                    }
                    if (asset.getNotes() != null && !asset.getNotes().isEmpty()) {
                        data += "\nGhi chú: " + asset.getNotes();
                    }
                    deliveryDataList.add(data);
                }

                preOrder.setStatus("COMPLETED");
                preOrder.setDeliveryData(String.join("\n\n", deliveryDataList));
                preOrderRepository.save(preOrder);

                // Notify Customer
                com.mmo.shared.model.Notification customerNotif = com.mmo.shared.model.Notification.builder()
                        .userId(preOrder.getCustomer().getId())
                        .title("Đơn đặt trước đã có hàng!")
                        .content("Sản phẩm '" + product.getName() + "' (Mã PO-" + preOrder.getId()
                                + ") đã được giao đủ " + needed
                                + " tài khoản. Vui lòng kiểm tra chi tiết đơn đặt trước của bạn.")
                        .type("ORDER")
                        .severity("SUCCESS")
                        .isRead(false)
                        .isDelete(false)
                        .createdAt(java.time.LocalDateTime.now())
                        .targetUrl("/pre-orders")
                        .build();
                notificationRepository.save(customerNotif);

                // Notify Seller
                com.mmo.shared.model.Notification sellerNotif = com.mmo.shared.model.Notification.builder()
                        .userId(product.getSeller().getId())
                        .title("Đã tự động gửi hàng cho khách!")
                        .content("Hệ thống đã tự động giao đủ " + needed
                                + " tài khoản cho đơn PO-" + preOrder.getId()
                                + " của sản phẩm '" + product.getName() + "'.")
                        .type("ORDER")
                        .severity("SUCCESS")
                        .isRead(false)
                        .isDelete(false)
                        .createdAt(java.time.LocalDateTime.now())
                        .targetUrl("/seller/preorders")
                        .build();
                notificationRepository.save(sellerNotif);
            }
        }

        synchronizeVariantStock(variant);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcilePendingPreOrdersOnStartup() {
        Set<Long> processedVariantIds = new HashSet<>();

        for (PreOrder preOrder : preOrderRepository.findByIsDeleteFalseOrderByCreatedAtAsc()) {
            if (!isPendingStatus(preOrder.getStatus())) {
                continue;
            }

            ProductVariant variant = preOrder.getVariant();
            if (variant == null) {
                List<ProductVariant> variants =
                        productVariantRepository.findByProductAndIsDeleteFalse(preOrder.getProduct());
                if (variants.size() != 1) {
                    log.warn("Không thể tự đối soát preorder {} vì chưa xác định được biến thể.", preOrder.getId());
                    continue;
                }
                variant = variants.get(0);
                preOrder.setVariant(variant);
            }

            if (processedVariantIds.add(variant.getId())) {
                autoFulfillPreOrders(variant, java.util.Collections.emptyList());
            }
        }
    }

    private boolean isPendingStatus(String status) {
        return status != null
                && ("Pending".equalsIgnoreCase(status) || "Chờ xử lý".equalsIgnoreCase(status));
    }

    private void synchronizeVariantStock(ProductVariant variant) {
        long availableCount =
                digitalAssetRepository.countByVariantAndIsUsedFalseAndIsDeleteFalse(variant);
        variant.setStock((int) Math.min(availableCount, Integer.MAX_VALUE));
        productVariantRepository.save(variant);
    }

    private ProductVariant resolveRequestedVariant(Product product, Long variantId) {
        if (variantId != null) {
            ProductVariant variant = productVariantRepository.findByIdAndIsDeleteFalse(variantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể."));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Biến thể không thuộc sản phẩm đã chọn.");
            }
            return variant;
        }

        List<ProductVariant> variants = productVariantRepository.findByProductAndIsDeleteFalse(product);
        if (variants.size() == 1) {
            return variants.get(0);
        }
        if (variants.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn chính xác biến thể cần đặt trước.");
        }
        return null;
    }

    private Transaction createEscrowTransactionForPreOrder(PreOrder preOrder) {
        User seller = preOrder.getProduct().getSeller();
        
        // 1. Đọc các số liệu shop level & dispute rate
        int shopLevel = seller.getShopLevel() != null ? seller.getShopLevel() : 1;
        long completedCount = transactionRepository.countCompletedSalesBySeller(seller);
        long totalSold = transactionRepository.countTotalSalesBySeller(seller);
        long resolvedComplaints = complaintRepository.countResolvedComplaintsBySeller(seller);
        
        double disputeRate = totalSold > 0 ? (double) resolvedComplaints / totalSold : 0.0;
        
        // 2. Tính toán số giờ giam tiền
        int escrowHoldHours = 72;
        if (shopLevel == 0 || completedCount < 20 || disputeRate >= 0.02) {
            escrowHoldHours = 168; // 7 ngày
        } else {
            escrowHoldHours = systemConfigurationRepository.findByConfigKey("ESCROW_HOLD_HOURS")
                    .map(c -> {
                        try { return Integer.parseInt(c.getConfigValue()); }
                        catch (NumberFormatException e) { return 72; }
                    }).orElse(72);
        }

        // 3. Tính phí hoa hồng động
        double basePercent = systemConfigurationRepository.findByConfigKey("DEFAULT_COMMISSION_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 5.0; }
                }).orElse(5.0);
        long totalAmount = preOrder.getExpectedPriceVnd();
        long commission = (long) (totalAmount * (basePercent / 100.0));

        // 4. Tạo thực thể Transaction mới
        Transaction transaction = Transaction.builder()
                .customer(preOrder.getCustomer())
                .seller(seller)
                .product(preOrder.getProduct())
                .variant(preOrder.getVariant())
                .amountVnd(totalAmount)
                .commissionVnd(commission)
                .quantity(preOrder.getQuantity())
                .status("Held")
                .paymentMethod("Wallet")
                .escrowReleaseDate(java.time.LocalDateTime.now().plusHours(escrowHoldHours))
                .isDelete(false)
                .build();

        return transactionRepository.save(transaction);
    }
}
