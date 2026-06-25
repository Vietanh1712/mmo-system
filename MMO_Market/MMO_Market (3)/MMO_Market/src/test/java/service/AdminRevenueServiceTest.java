package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import controller.dto.RevenueSummaryResponse;
import dal.*;
import model.*;
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
        when(sellerRegistrationRepository.countByStatusAndIsDeleteFalse("Approved")).thenReturn(10L);
        when(productRepository.countByIsDeleteFalse()).thenReturn(50L);
        when(systemConfigurationRepository.findByConfigKey("SELLER_UPGRADE_FEE_VND")).thenReturn(Optional.empty()); // default 50000L
        when(systemConfigurationRepository.findByConfigKey("PRODUCT_FEATURED_FEE_VND")).thenReturn(Optional.empty()); // default 10000L
        when(systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")).thenReturn(Optional.empty()); // default 1.5
        when(systemConfigurationRepository.findByConfigKey("MIN_WITHDRAW_FEE_VND")).thenReturn(Optional.empty()); // default 10000L

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
        assertEquals(500000L, summary.getSellerUpgradeFees()); // 10 * 50000
        assertEquals(100000L, summary.getProductFeaturedFees()); // (50 * 0.2) * 10000
        assertEquals(15000L, summary.getWithdrawalFees());
        assertEquals(1115000L, summary.getNetTotal());
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
    }

    @Test
    void getCashflowTransactionsFetchesAllTypesCorrectly() {
        User admin = User.builder()
                .id(1L)
                .email("admin@mmo.com")
                .role("{\"role\": \"Admin\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        TopupTransaction topup = TopupTransaction.builder()
                .id(10L)
                .userId(3L)
                .amountVnd(100000L)
                .status("Completed")
                .createdAt(LocalDateTime.now())
                .isDelete(false)
                .build();
        when(topupTransactionRepository.findAllByIsDeleteFalse()).thenReturn(List.of(topup));

        User seller = User.builder()
                .id(3L)
                .email("seller@mmo.com")
                .isDelete(false)
                .build();
        when(userRepository.findAllById(Collections.singleton(3L))).thenReturn(List.of(seller));

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

        when(systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")).thenReturn(Optional.empty());
        when(systemConfigurationRepository.findByConfigKey("MIN_WITHDRAW_FEE_VND")).thenReturn(Optional.empty());

        Map<String, Object> result = service.getCashflowTransactions(1L, "", "", "", 0, 10);

        assertNotNull(result);
        List<?> content = (List<?>) result.get("content");
        assertEquals(3, content.size());
        assertEquals(3, result.get("totalElements"));
    }
}
