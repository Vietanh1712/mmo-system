package com.mmo.feature.order.service;

import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionStatusSyncService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public void markPendingPayment(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));
        if (!"Unpaid".equalsIgnoreCase(tx.getStatus())) {
            throw new IllegalStateException("Giao dịch phải ở trạng thái Unpaid mới có thể chuyển sang Pending.");
        }
        tx.setStatus("Pending");
        transactionRepository.save(tx);
    }

    @Transactional
    public void markPaid(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));
        tx.setStatus("Paid");
        tx.setCreatedAt(LocalDateTime.now()); // Hoặc paidAt
        transactionRepository.save(tx);
    }

    @Transactional
    public void syncFromPaymentFailed(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với ID: " + transactionId));
        tx.setStatus("Unpaid");
        transactionRepository.save(tx);
    }
}
