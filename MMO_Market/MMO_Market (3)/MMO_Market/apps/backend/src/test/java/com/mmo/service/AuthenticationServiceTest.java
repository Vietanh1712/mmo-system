package com.mmo.service;

import com.mmo.feature.auth.service.AuthenticationService;
import com.mmo.feature.auth.service.EmailService;
import com.mmo.shared.dal.AuthenticationRepository;
import com.mmo.shared.dal.EmailVerificationRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dto.*;
import com.mmo.shared.model.EmailVerification;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.User;
import com.mmo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "googleClientId", "mock-client-id");
        ReflectionTestUtils.setField(authenticationService, "googleClientSecret", "mock-client-secret");
    }

    @Test
    void login_suspendedUser_denied() {
        User suspended = new User();
        suspended.setId(10L);
        suspended.setEmail("suspended@mmo.com");
        suspended.setIsLocked(true);
        when(userRepository.findByEmail("suspended@mmo.com")).thenReturn(Optional.of(suspended));

        LoginRequest request = new LoginRequest();
        request.setEmail("suspended@mmo.com");
        request.setPassword("password");

        LoginResponse response = authenticationService.login(request);
        assertNotNull(response);
        assertFalse(response.isSuccess());
    }

    @Test
    void login_activeUser_returnsTokens() {
        User active = new User();
        active.setId(10L);
        active.setEmail("active@mmo.com");
        active.setIsLocked(false);
        active.setIsVerified(true);
        active.setPassword("hashedPassword");
        
        when(userRepository.findByEmail("active@mmo.com")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(10L, active.getEmail(), "Customer")).thenReturn("mockAccessToken");

        LoginRequest request = new LoginRequest();
        request.setEmail("active@mmo.com");
        request.setPassword("password");

        LoginResponse response = authenticationService.login(request);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("mockAccessToken", response.getAccessToken());
    }

    @Test
    void refreshToken_revoked_denied() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("revokedToken");

        TokenRefreshResponse response = authenticationService.refreshToken(request);
        assertNotNull(response);
        assertFalse(response.isSuccess());
    }
}
