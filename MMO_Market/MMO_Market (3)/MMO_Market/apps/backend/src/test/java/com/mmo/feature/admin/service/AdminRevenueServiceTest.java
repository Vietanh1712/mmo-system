package com.mmo.feature.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dto.RevenueSummaryResponse;
import com.mmo.shared.dal.*;
import com.mmo.shared.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRevenueServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private TopupTransactionRepository topupTransactionRepository;

    @Mock
    private SellerRegistrationRepository sellerRegistrationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    private AdminRevenueService service;

    @BeforeEach
    void setUp() {
        service = new AdminRevenueService(
                transactionRepository,
                withdrawalRepository,
                topupTransactionRepository,
                sellerRegistrationRepository,
                productRepository,
                userRepository,
                systemConfigurationRepository,
                new ObjectMapper()
        );
    }

    @Test
    void getRevenueSummarySucceedsForAdmin() {
        User admin = User.builder()
                .id(1L)
                .email("admin@mmo.com")
                .role("{\"role\": \"Admin\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(transactionRepository.sumCommissionForCompletedOrHeldTransactions()).thenReturn(500000L);

        SellerRegistration reg1 = SellerRegistration.builder().id(1L).status("Approved").feeVnd(300000L).build();
        SellerRegistration reg2 = SellerRegistration.builder().id(2L).status("Approved").feeVnd(200000L).build();
        when(sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc()).thenReturn(List.of(reg1, reg2));

        when(systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")).thenReturn(Optional.of(new SystemConfiguration(1, "SHOP_OPENING_FEE_VND", "1000000", "Phí mở shop", null, null)));
        when(systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")).thenReturn(Optional.empty()); // default 1.5

        Withdrawal completedWithdrawal = Withdrawal.builder()
                .id(1L)
                .amountVnd(1000000L)
                .feeVnd(15000L)
                .status("Completed")
                .isDelete(false)
                .build();
        when(withdrawalRepository.findByStatusAndIsDeleteFalse("Completed")).thenReturn(List.of(completedWithdrawal));

        RevenueSummaryResponse summary = service.getRevenueSummary(1L);

        assertNotNull(summary);
        assertEquals(500000L, summary.getCommissions());
        assertEquals(500000L, summary.getShopOpeningFees()); // 300000 + 200000 (Uses historical feeVnd, ignores current config 1,000,000)
        assertEquals(15000L, summary.getWithdrawalFees());
        assertEquals(1015000L, summary.getNetTotal());
    }

    @Test
    void getRevenueSummaryThrowsForbiddenForNonAdmin() {
        User customer = User.builder()
                .id(2L)
                .email("customer@mmo.com")
                .role("{\"role\": \"Customer\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(customer));

        assertThrows(ResponseStatusException.class, () -> service.getRevenueSummary(2L));
    }    @Test
    void getCashflowTransactionsFetchesAllTypesCorrectly() {
        User admin = User.builder()
                .id(1L)
                .email("admin@mmo.com")
                .role("{\"role\": \"Admin\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SellerRegistration reg = SellerRegistration.builder()
                .id(100L)
                .status("Approved")
                .feeVnd(500000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc()).thenReturn(List.of(reg));

        User seller = User.builder()
                .id(3L)
                .email("seller@mmo.com")
                .isDelete(false)
                .build();

        Withdrawal withdrawal = Withdrawal.builder()
                .id(20L)
                .seller(seller)
                .amountVnd(200000L)
                .feeVnd(10000L)
                .status("Completed")
                .createdAt(LocalDateTime.now().minusHours(1))
                .isDelete(false)
                .build();
        when(withdrawalRepository.findAllWithSellerByIsDeleteFalse()).thenReturn(List.of(withdrawal));

        Transaction transaction = Transaction.builder()
                .id(30L)
                .customer(seller)
                .amountVnd(500000L)
                .commissionVnd(25000L)
                .status("Completed")
                .createdAt(LocalDateTime.now().minusHours(2))
                .isDelete(false)
                .build();
        when(transactionRepository.findAllWithCustomerByIsDeleteFalse()).thenReturn(List.of(transaction));

        when(systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")).thenReturn(Optional.empty());
        when(systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")).thenReturn(Optional.empty());

        Map<String, Object> result = service.getCashflowTransactions(1L, "", "", "", "", 0, 10);

        assertNotNull(result);
        List<?> content = (List<?>) result.get("content");
        assertEquals(3, content.size());
        assertEquals(3, result.get("totalElements"));
    }

    @Test
    void getCashflowTransactionsFiltersByDateRange() {
        User admin = User.builder()
                .id(1L)
                .email("admin@mmo.com")
                .role("{\"role\": \"Admin\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        LocalDateTime now = LocalDateTime.of(2026, 6, 25, 12, 0, 0);

        SellerRegistration reg1 = SellerRegistration.builder()
                .id(10L)
                .status("Approved")
                .feeVnd(500000L)
                .createdAt(now.minusDays(5)) // 2026-06-20
                .isDelete(false)
                .build();

        SellerRegistration reg2 = SellerRegistration.builder()
                .id(11L)
                .status("Approved")
                .feeVnd(500000L)
                .createdAt(now) // 2026-06-25
                .isDelete(false)
                .build();

        when(sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc()).thenReturn(List.of(reg1, reg2));
        when(withdrawalRepository.findAllWithSellerByIsDeleteFalse()).thenReturn(Collections.emptyList());
        when(transactionRepository.findAllWithCustomerByIsDeleteFalse()).thenReturn(Collections.emptyList());
        when(systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")).thenReturn(Optional.empty());
        when(systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")).thenReturn(Optional.empty());

        // Lọc trong khoảng 2026-06-21 đến 2026-06-26 -> chỉ có reg2
        Map<String, Object> result = service.getCashflowTransactions(1L, "", "", "2026-06-21", "2026-06-26", 0, 10);

        assertNotNull(result);
        List<?> content = (List<?>) result.get("content");
        assertEquals(1, content.size());
    }
}

