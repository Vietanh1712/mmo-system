package com.mmo.feature.preorder.controller;
import com.mmo.shared.model.PreOrder;

import com.mmo.shared.dto.PreOrderRequest;
import com.mmo.shared.dto.PreOrderResponse;
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
}
