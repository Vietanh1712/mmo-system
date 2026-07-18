package com.mmo.feature.wallet.service;

import com.mmo.shared.dto.WalletStatsDto;
import com.mmo.shared.dto.WalletTransactionDto;
import com.mmo.shared.dal.WalletTransactionRepository;
import com.mmo.shared.model.User;
import com.mmo.shared.model.WalletTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WalletService {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    public WalletStatsDto getWalletStats(Long userId) {
        List<WalletTransaction> transactions = walletTransactionRepository.findByUser_IdAndIsDeleteFalse(userId);
        
        long totalTopup = 0;
        long totalSpent = 0;
        long pendingCount = 0;
        long escrowAmount = 0;

        for (WalletTransaction txn : transactions) {
            if (("TOPUP".equals(txn.getType()) || "REFUND".equals(txn.getType())) && "SUCCESS".equals(txn.getStatus())) {
                totalTopup += txn.getAmountVnd();
            }
            if ("PAYMENT".equals(txn.getType()) && "SUCCESS".equals(txn.getStatus())) {
                totalSpent += Math.abs(txn.getAmountVnd());
            }
            if ("PENDING".equals(txn.getStatus())) {
                pendingCount++;
            }
            if ("ESCROW".equals(txn.getType())) {
                escrowAmount += Math.abs(txn.getAmountVnd());
            }
        }

        return WalletStatsDto.builder()
                .totalTopup(totalTopup)
                .totalSpent(totalSpent)
                .pendingCount(pendingCount)
                .escrowAmount(escrowAmount)
                .build();
    }

    public Page<WalletTransactionDto> getTransactions(Long userId, Pageable pageable) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return walletTransactionRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId, pageable)
                .map(txn -> WalletTransactionDto.builder()
                        .code(txn.getReferenceCode())
                        .type(txn.getType())
                        .amount(txn.getAmountVnd())
                        .status(txn.getStatus())
                        .description(txn.getDescription())
                        .createdAt(txn.getCreatedAt() != null ? txn.getCreatedAt().format(formatter) : "")
                        .build());
    }

    public WalletTransaction recordTransaction(User user, String type, Long amountVnd, String status, String description, String referenceCode, Long balanceAfter) {
        return recordTransaction(user, type, amountVnd, status, description, referenceCode, balanceAfter, null);
    }

    public WalletTransaction recordTransaction(User user, String type, Long amountVnd, String status, String description, String referenceCode, Long balanceAfter, Long referenceId) {
        String transactionType = (amountVnd != null && amountVnd >= 0) ? "IN" : "OUT";
        WalletTransaction txn = WalletTransaction.builder()
                .user(user)
                .type(type)
                .amountVnd(amountVnd)
                .status(status)
                .description(description)
                .referenceCode(referenceCode)
                .balanceAfter(balanceAfter != null ? balanceAfter : 0L)
                .transactionType(transactionType)
                .referenceId(referenceId)
                .build();
        return walletTransactionRepository.save(txn);
    }
}
