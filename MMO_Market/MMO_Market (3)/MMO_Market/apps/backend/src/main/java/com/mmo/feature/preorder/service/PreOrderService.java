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

    public PreOrderService(PreOrderRepository preOrderRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository) {
        this.preOrderRepository = preOrderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
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
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().format(formatter) : "")
                .build();
    }
}
