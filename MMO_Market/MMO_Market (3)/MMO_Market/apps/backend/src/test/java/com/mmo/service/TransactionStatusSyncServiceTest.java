package com.mmo.service;

import com.mmo.feature.order.service.TransactionStatusSyncService;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LỚP KIỂM THỬ: TransactionStatusSyncServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionStatusSyncServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionStatusSyncService transactionStatusSyncService;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setId(100L);
        transaction.setStatus("Unpaid");
        transaction.setIsDelete(false);
    }

    /**
     * Ca kiểm thử: Mark pending thanh toán from unpaid.
     */
    @Test
    void markPendingPayment_fromUnpaid() {
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionStatusSyncService.markPendingPayment(100L);

        assertEquals("Pending", transaction.getStatus());
        verify(transactionRepository).save(transaction);
    }

    /**
     * Ca kiểm thử: Mark paid sets paid at and logs.
     */
    @Test
    void markPaid_setsPaidAtAndLogs() {
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionStatusSyncService.markPaid(100L);

        assertEquals("Paid", transaction.getStatus());
        assertNotNull(transaction.getCreatedAt()); // Lưu vết thời gian thanh toán/tạo giao dịch
        verify(transactionRepository).save(transaction);
    }

    /**
     * Ca kiểm thử: Sync from thanh toán thất bại resets to unpaid.
     */
    @Test
    void syncFromPayment_failedResetsToUnpaid() {
        transaction.setStatus("Pending");
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionStatusSyncService.syncFromPaymentFailed(100L);

        assertEquals("Unpaid", transaction.getStatus());
        verify(transactionRepository).save(transaction);
    }
}
