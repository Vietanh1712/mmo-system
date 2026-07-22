package com.mmo.feature.preorder.controller;
import com.mmo.shared.model.PreOrder;

import com.mmo.shared.dto.PreOrderRequest;
import com.mmo.shared.dto.PreOrderResponse;
import com.mmo.shared.dto.PreOrderDeliveryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.preorder.service.PreOrderService;

@RestController
@RequestMapping("/api/v1/pre-orders")
public class PreOrderController {

    private final PreOrderService preOrderService;

    public PreOrderController(PreOrderService preOrderService) {
        this.preOrderService = preOrderService;
    }

    @PostMapping
    public ResponseEntity<PreOrderResponse> createPreOrder(@AuthenticationPrincipal Long userId,
                                                           @Valid @RequestBody PreOrderRequest request) {
        PreOrderResponse response = preOrderService.createPreOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PreOrderResponse>> getPreOrders(@AuthenticationPrincipal Long userId) {
        List<PreOrderResponse> response = preOrderService.getPreOrdersByCustomer(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller")
    public ResponseEntity<List<PreOrderResponse>> getSellerPreOrders(@AuthenticationPrincipal Long userId) {
        List<PreOrderResponse> response = preOrderService.getPreOrdersBySeller(userId);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/seller/{id}/status")
    public ResponseEntity<PreOrderResponse> updatePreOrderStatus(@AuthenticationPrincipal Long userId,
                                                                 @org.springframework.web.bind.annotation.PathVariable("id") Long preOrderId,
                                                                 @RequestBody java.util.Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        PreOrderResponse response = preOrderService.updatePreOrderStatus(userId, preOrderId, newStatus.trim());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/seller/{preOrderId}/deliver")
    public ResponseEntity<PreOrderResponse> deliverPreOrder(@AuthenticationPrincipal Long userId,
                                                            @PathVariable Long preOrderId,
                                                            @Valid @RequestBody PreOrderDeliveryRequest request) {
        PreOrderResponse response = preOrderService.deliverPreOrder(userId, preOrderId, request.getDeliveryData());
        return ResponseEntity.ok(response);
    }
}
