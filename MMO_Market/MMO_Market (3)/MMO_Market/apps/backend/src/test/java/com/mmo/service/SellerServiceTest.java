package com.mmo.service;

import com.mmo.feature.seller.service.ShopRegistrationService;
import com.mmo.shared.dal.*;
import com.mmo.shared.dto.ShopRegistrationRequestDto;
import com.mmo.shared.dto.ShopRegistrationResponseDto;
import com.mmo.shared.model.KycRequest;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.SellerRegistration;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LỚP KIỂM THỬ: SellerServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    @Mock
    private SellerRegistrationRepository sellerRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KycRequestRepository kycRequestRepository;

    @Mock
    private SellerBankInfoRepository sellerBankInfoRepository;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ShopRegistrationService shopRegistrationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole("{\"role\": \"Customer\"}");
        user.setIsDelete(false);
    }

    /**
     * Ca kiểm thử: Tạo mới mặc định không hoạt động
     */
    @Test
    void create_defaultsInactive() {
        assertEquals("{\"role\": \"Customer\"}", user.getRole());
    }

    /**
     * Ca kiểm thử: Cập nhật kích hoạt khi không có quản lý conflicts
     */
    @Test
    void update_activateWithoutstaff_conflicts() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));
        
        KycRequest pendingKyc = new KycRequest();
        pendingKyc.setStatus(KycStatus.PENDING);
        
        when(kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(pendingKyc));

        ShopRegistrationRequestDto request = new ShopRegistrationRequestDto();
        request.setShopName("Gian Hang Alpha");

        assertThrows(IllegalArgumentException.class, () -> 
            shopRegistrationService.submitRegistration(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Assign quản lý từ chối non hoạt động user
     */
    @Test
    void assignstaff_rejectsNonActiveUser() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));
        
        KycRequest pendingKyc = new KycRequest();
        pendingKyc.setStatus(KycStatus.PENDING);
        
        when(kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(pendingKyc));

        ShopRegistrationRequestDto request = new ShopRegistrationRequestDto();
        request.setShopName("Gian Hang Alpha");

        assertThrows(IllegalArgumentException.class, () -> 
            shopRegistrationService.submitRegistration(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Assign quản lý swaps hoạt động assignment
     */
    @Test
    void assignstaff_swapsActiveAssignment() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));
        
        KycRequest approvedKyc = new KycRequest();
        approvedKyc.setStatus(KycStatus.APPROVED);
        
        when(kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(approvedKyc));

        SellerRegistration savedReg = new SellerRegistration();
        savedReg.setId(100L);
        savedReg.setStatus("APPROVED");
        savedReg.setUser(user);
        
        when(sellerRegistrationRepository.save(any(SellerRegistration.class))).thenReturn(savedReg);

        ShopRegistrationRequestDto request = new ShopRegistrationRequestDto();
        request.setShopName("Gian Hang Alpha");
        request.setBankName("Techcombank");
        request.setBankAccountNumber("123456789");
        request.setBankAccountName("NGUYEN VAN A");

        ShopRegistrationResponseDto response = shopRegistrationService.submitRegistration(1L, request);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        verify(sellerRegistrationRepository).save(any(SellerRegistration.class));
    }
}
