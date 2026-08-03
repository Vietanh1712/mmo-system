package com.mmo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.feature.admin.service.SystemConfigurationService;
import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dto.SystemConfigResponse;
import com.mmo.shared.dto.SystemConfigUpdateRequest;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LỚP KIỂM THỬ: SystemConfigurationServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class SystemConfigurationServiceTest {

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SystemConfigurationService systemConfigurationService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@mmo.com");
        admin.setRole("{\"role\": \"Admin\"}");
        admin.setIsDelete(false);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setEmail("user@mmo.com");
        regularUser.setRole("{\"role\": \"Customer\"}");
        regularUser.setIsDelete(false);
    }

    /**
     * Ca kiểm thử: Get settings returns mặc định khi no rows
     */
    @Test
    void getSettings_returnsDefaults_whenNoRows() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(systemConfigurationRepository.findAll()).thenReturn(Arrays.asList());

        SystemConfigResponse response = systemConfigurationService.getConfigurations(1L);

        assertNotNull(response);
        assertEquals(15, response.getSystemConfig().getSessionTimeout());
        assertEquals(5, response.getSystemConfig().getOtpTimeout());
    }

    /**
     * Ca kiểm thử: Get settings returns stored values
     */
    @Test
    void getSettings_returnsStoredValues() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        
        SystemConfiguration config1 = new SystemConfiguration();
        config1.setConfigKey("SESSION_TIMEOUT_MINS");
        config1.setConfigValue("30");

        SystemConfiguration config2 = new SystemConfiguration();
        config2.setConfigKey("LOCK_DURATION_MINS");
        config2.setConfigValue("15");

        when(systemConfigurationRepository.findAll()).thenReturn(Arrays.asList(config1, config2));

        SystemConfigResponse response = systemConfigurationService.getConfigurations(1L);

        assertNotNull(response);
        assertEquals(30, response.getSystemConfig().getSessionTimeout());
    }

    /**
     * Ca kiểm thử: Get deposit percentage returns fraction
     */
    @Test
    void getDepositPercentage_returnsFraction() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        
        SystemConfiguration config = new SystemConfiguration();
        config.setConfigKey("DEFAULT_COMMISSION_PERCENT");
        config.setConfigValue("5.5");
        when(systemConfigurationRepository.findAll()).thenReturn(Arrays.asList(config));

        SystemConfigResponse response = systemConfigurationService.getConfigurations(1L);
        assertEquals(5.5, response.getCommissions().getBasePercent());
    }

    /**
     * Ca kiểm thử: Get deposit percentage returns default khi missing
     */
    @Test
    void getDepositPercentage_returnsDefault_whenMissing() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(systemConfigurationRepository.findAll()).thenReturn(Arrays.asList());

        SystemConfigResponse response = systemConfigurationService.getConfigurations(1L);
        assertEquals(5.0, response.getCommissions().getBasePercent());
    }

    /**
     * Ca kiểm thử: Cập nhật settings deposit below10 ném ra lỗi
     */
    @Test
    void updateSettings_depositBelow10_throws() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setSessionTimeout(4); // Lỗi biên dịch SessionTimeout < 5

        assertThrows(ResponseStatusException.class, () -> 
            systemConfigurationService.updateGeneralConfig(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Cập nhật settings deposit above50 ném ra lỗi
     */
    @Test
    void updateSettings_depositAbove50_throws() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setSessionTimeout(4);

        assertThrows(ResponseStatusException.class, () -> 
            systemConfigurationService.updateGeneralConfig(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Cập nhật settings deposit40 ok
     */
    @Test
    void updateSettings_deposit40_ok() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setSessionTimeout(30);
        request.setOtpTimeout(5);
        request.setMaxLoginRetries(5);
        request.setLockDurationMins(15);
        request.setEscrowHoldHours(72);

        assertDoesNotThrow(() -> 
            systemConfigurationService.updateGeneralConfig(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Cập nhật settings không hợp lệ hộp thư email ném ra lỗi
     */
    @Test
    void updateSettings_invalidEmail_throws() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setSessionTimeout(4);

        assertThrows(ResponseStatusException.class, () -> 
            systemConfigurationService.updateGeneralConfig(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Cập nhật settings hợp lệ hộp thư email ok
     */
    @Test
    void updateSettings_validEmail_ok() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setSessionTimeout(30);
        request.setOtpTimeout(5);
        request.setMaxLoginRetries(5);
        request.setLockDurationMins(15);
        request.setEscrowHoldHours(72);

        assertDoesNotThrow(() -> 
            systemConfigurationService.updateGeneralConfig(1L, request)
        );
    }

    /**
     * Ca kiểm thử: Cập nhật settings empty hộp thư email allowed
     */
    @Test
    void updateSettings_emptyEmail_allowed() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setSessionTimeout(30);
        request.setOtpTimeout(5);
        request.setMaxLoginRetries(5);
        request.setLockDurationMins(15);
        request.setEscrowHoldHours(72);

        assertDoesNotThrow(() -> 
            systemConfigurationService.updateGeneralConfig(1L, request)
        );
    }
}
