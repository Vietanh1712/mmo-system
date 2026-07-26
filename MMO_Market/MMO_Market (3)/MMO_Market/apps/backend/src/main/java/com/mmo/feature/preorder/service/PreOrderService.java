package com.mmo.feature.preorder.service;
import com.mmo.shared.model.Transaction;

import com.mmo.shared.dto.PreOrderRequest;
import com.mmo.shared.dto.PreOrderResponse;
import com.mmo.shared.dal.PreOrderRepository;
import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.PreOrder;
import com.mmo.shared.model.Product;
import com.mmo.shared.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PreOrderService {

    private final PreOrderRepository preOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final com.mmo.shared.dal.DigitalAssetRepository digitalAssetRepository;
    private final com.mmo.shared.dal.NotificationRepository notificationRepository;

    public PreOrderService(PreOrderRepository preOrderRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           com.mmo.shared.dal.DigitalAssetRepository digitalAssetRepository,
                           com.mmo.shared.dal.NotificationRepository notificationRepository) {
        this.preOrderRepository = preOrderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
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

        PreOrder preOrder = new PreOrder();
        preOrder.setCustomer(customer);
        preOrder.setProduct(product);
        preOrder.setQuantity(request.getQuantity());
        preOrder.setExpectedPriceVnd(request.getExpectedPriceVnd());
        preOrder.setNotes(request.getNotes() == null ? null : request.getNotes().trim());

        PreOrder saved = preOrderRepository.save(preOrder);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return PreOrderResponse.builder()
                .success(true)
                .message("Đã gửi yêu cầu đặt trước thành công.")
                .id(saved.getId())
                .productId(product.getId())
                .productName(product.getName())
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
                        .quantity(po.getQuantity())
                        .expectedPriceVnd(po.getExpectedPriceVnd())
                        .status(po.getStatus())
                        .notes(po.getNotes())
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
                        .customerEmail(po.getCustomer().getEmail())
                        .quantity(po.getQuantity())
                        .expectedPriceVnd(po.getExpectedPriceVnd())
                        .status(po.getStatus())
                        .notes(po.getNotes())
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

        preOrder.setStatus(newStatus);
        PreOrder saved = preOrderRepository.save(preOrder);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return PreOrderResponse.builder()
                .success(true)
                .message("Cập nhật trạng thái thành công.")
                .id(saved.getId())
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getName())
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
    public PreOrderResponse deliverPreOrder(Long sellerId, Long preOrderId, String deliveryData) {
        PreOrder preOrder = preOrderRepository.findById(preOrderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt trước"));

        if (!preOrder.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác này");
        }

        if (!preOrder.getStatus().equalsIgnoreCase("PENDING") && !preOrder.getStatus().equalsIgnoreCase("Chờ xử lý")) {
            throw new RuntimeException("Chỉ có thể trả hàng cho đơn đang chờ xử lý");
        }

        preOrder.setStatus("COMPLETED");
        preOrder.setDeliveryData(deliveryData);
        PreOrder saved = preOrderRepository.save(preOrder);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return PreOrderResponse.builder()
                .id(saved.getId())
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getName())
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
    public void autoFulfillPreOrders(com.mmo.shared.model.ProductVariant variant, List<com.mmo.shared.model.DigitalAsset> newAssets) {
        if (newAssets == null || newAssets.isEmpty()) return;

        Product product = variant.getProduct();
        List<PreOrder> pendingPreOrders = preOrderRepository.findByProductAndStatusIgnoreCaseAndIsDeleteFalseOrderByCreatedAtAsc(product, "Pending");

        if (pendingPreOrders.isEmpty()) {
            pendingPreOrders = preOrderRepository.findByProductAndStatusIgnoreCaseAndIsDeleteFalseOrderByCreatedAtAsc(product, "Chờ xử lý");
        }

        if (pendingPreOrders.isEmpty()) return;

        // Collect all available un-used assets from these newly added assets
        List<com.mmo.shared.model.DigitalAsset> availableAssets = new java.util.ArrayList<>();
        for (com.mmo.shared.model.DigitalAsset asset : newAssets) {
            if (asset.getIsUsed() == null || !asset.getIsUsed()) {
                availableAssets.add(asset);
            }
        }

        for (PreOrder preOrder : pendingPreOrders) {
            if (availableAssets.isEmpty()) break;

            int needed = preOrder.getQuantity() != null ? preOrder.getQuantity() : 1;
            if (availableAssets.size() >= needed) {
                // Fulfill this preorder
                List<String> deliveryDataList = new java.util.ArrayList<>();
                for (int i = 0; i < needed; i++) {
                    com.mmo.shared.model.DigitalAsset asset = availableAssets.remove(0);
                    asset.setIsUsed(true);
                    digitalAssetRepository.save(asset);

                    String data = "";
                    if (asset.getAssetType().equalsIgnoreCase("ACCOUNT")) {
                        data = "Tài khoản: " + asset.getAccountUsername() + " | Mật khẩu: " + asset.getAccountPassword();
                    } else if (asset.getAssetType().equalsIgnoreCase("KEY")) {
                        data = "Mã Key: " + asset.getKeyCode();
                    } else if (asset.getAssetType().equalsIgnoreCase("CARD")) {
                        data = "Mã thẻ: " + asset.getCardCode() + " | Seri: " + asset.getCardPin();
                    }
                    if (asset.getNotes() != null && !asset.getNotes().isEmpty()) {
                        data += " | Ghi chú: " + asset.getNotes();
                    }
                    deliveryDataList.add(data);
                }

                preOrder.setStatus("COMPLETED");
                preOrder.setDeliveryData(String.join("\n", deliveryDataList));
                preOrderRepository.save(preOrder);

                // Notify Customer
                com.mmo.shared.model.Notification customerNotif = com.mmo.shared.model.Notification.builder()
                        .userId(preOrder.getCustomer().getId())
                        .title("Đơn đặt trước đã có hàng!")
                        .content("Sản phẩm '" + product.getName() + "' (Mã PO-" + preOrder.getId() + ") đã được người bán gửi hàng. Vui lòng kiểm tra chi tiết đơn đặt trước của bạn.")
                        .type("ORDER")
                        .severity("SUCCESS")
                        .isRead(false)
                        .isDelete(false)
                        .createdAt(java.time.LocalDateTime.now())
                        .targetUrl("/preorders")
                        .build();
                notificationRepository.save(customerNotif);

                // Notify Seller
                com.mmo.shared.model.Notification sellerNotif = com.mmo.shared.model.Notification.builder()
                        .userId(product.getSeller().getId())
                        .title("Đã tự động gửi hàng cho khách!")
                        .content("Hệ thống đã tự động lấy tài sản vừa nạp để giao cho Đơn đặt trước PO-" + preOrder.getId() + " của sản phẩm '" + product.getName() + "'.")
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
    }
}
