package com.mmo.service;

import com.mmo.feature.seller.service.ShopLevelService;
import com.mmo.feature.wallet.service.TopupService;
import com.mmo.feature.wallet.service.WalletService;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.TopupTransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.WalletTransactionRepository;
import com.mmo.shared.dto.SePayWebhookRequest;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.TopupTransaction;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TopupTransactionRepository topupTransactionRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @Mock
    private ShopLevelService shopLevelService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private TopupService topupService;

    private SePayWebhookRequest validRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRequest = new SePayWebhookRequest();
        validRequest.setId(12345L);
        validRequest.setTransferType("in");
        validRequest.setTransferAmount(100000L);
        validRequest.setContent("MMO-TOPUP-10");

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("buyer@mmo.com");
        testUser.setBalanceVnd(50000L);
    }

    @Test
    void processSepayWebhook_NonInflowTransaction_ReturnsFalse() {
        SePayWebhookRequest outRequest = new SePayWebhookRequest();
        outRequest.setTransferType("out");

        boolean result = topupService.processSepayWebhook(outRequest);

        assertFalse(result);
        verify(topupTransactionRepository, never()).findBySepayCode(any());
    }

    @Test
    void processSepayWebhook_DuplicateSepayTransaction_SkipsAndReturnsTrue() {
        when(topupTransactionRepository.findBySepayCode("12345"))
                .thenReturn(Optional.of(new TopupTransaction()));

        boolean result = topupService.processSepayWebhook(validRequest);

        assertTrue(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void processSepayWebhook_EmptyContent_SavesFailedTransaction() {
        SePayWebhookRequest invalidRequest = new SePayWebhookRequest();
        invalidRequest.setId(12345L);
        invalidRequest.setTransferType("in");
        invalidRequest.setTransferAmount(100000L);
        invalidRequest.setContent("");

        when(topupTransactionRepository.findBySepayCode("12345")).thenReturn(Optional.empty());

        boolean result = topupService.processSepayWebhook(invalidRequest);

        assertFalse(result);
        ArgumentCaptor<TopupTransaction> captor = ArgumentCaptor.forClass(TopupTransaction.class);
        verify(topupTransactionRepository).save(captor.capture());
        assertEquals("Failed", captor.getValue().getStatus());
        assertEquals("Nội dung chuyển khoản rỗng.", captor.getValue().getFailureReason());
    }
}
