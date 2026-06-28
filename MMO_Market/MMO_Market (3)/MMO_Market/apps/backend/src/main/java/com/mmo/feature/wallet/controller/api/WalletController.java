package com.mmo.feature.wallet.controller.api;

import com.mmo.shared.dto.WalletStatsDto;
import com.mmo.shared.dto.WalletTransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.wallet.service.WalletService;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/stats")
    public ResponseEntity<?> getWalletStats(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        WalletStatsDto stats = walletService.getWalletStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getWalletTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionDto> transactions = walletService.getTransactions(userId, pageable);
        return ResponseEntity.ok(transactions);
    }
}
