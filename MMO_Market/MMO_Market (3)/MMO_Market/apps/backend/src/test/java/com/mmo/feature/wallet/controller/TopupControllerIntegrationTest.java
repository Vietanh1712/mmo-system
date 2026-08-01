package com.mmo.feature.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.TopupTransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dto.SePayWebhookRequest;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.TopupTransaction;
import com.mmo.shared.model.User;
import com.mmo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TopupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopupTransactionRepository topupTransactionRepository;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User customerUser;
    private User staffUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // The token defined in our test configuration / properties
    private final String sepayWebhookToken = "IR7WFW2P3SXCNHZOVTX9BD1BYKOKXHQ9MMZJF0OAY6EV85IPDGYNZJSACJKAELHA";

    @BeforeEach
    void setUp() {
        // Clear security context
        SecurityContextHolder.clearContext();

        // 1. Create a test Customer User
        customerUser = User.builder()
                .email("test_cust_topup@mmo.com")
                .password("$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq") // 123456
                .fullName("Test Customer Topup")
                .role("{\"role\":\"Customer\"}")
                .isVerified(true)
                .isLocked(false)
                .isDelete(false)
                .balanceVnd(50000L)
                .build();
        customerUser = userRepository.save(customerUser);

        // 2. Create a test Staff User
        staffUser = User.builder()
                .email("test_staff_topup@mmo.com")
                .password("$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq") // 123456
                .fullName("Test Staff Topup")
                .role("{\"role\":\"Staff\"}")
                .isVerified(true)
                .isLocked(false)
                .isDelete(false)
                .balanceVnd(0L)
                .build();
        staffUser = userRepository.save(staffUser);

        // Ensure config limits exist in database for validation
        if (systemConfigurationRepository.findByConfigKey("MIN_DEPOSIT_LIMIT_VND").isEmpty()) {
            systemConfigurationRepository.save(SystemConfiguration.builder()
                    .configKey("MIN_DEPOSIT_LIMIT_VND")
                    .configValue("10000")
                    .description("Min deposit limit")
                    .build());
        }
        if (systemConfigurationRepository.findByConfigKey("MAX_DEPOSIT_LIMIT_VND").isEmpty()) {
            systemConfigurationRepository.save(SystemConfiguration.builder()
                    .configKey("MAX_DEPOSIT_LIMIT_VND")
                    .configValue("50000000")
                    .description("Max deposit limit")
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (customerUser != null || staffUser != null) {
            Long custId = customerUser != null ? customerUser.getId() : -1L;
            Long stfId = staffUser != null ? staffUser.getId() : -1L;

            // Delete records referencing our test users
            jdbcTemplate.update("DELETE FROM Notifications WHERE user_id IN (?, ?)", custId, stfId);
            jdbcTemplate.update("DELETE FROM WalletTransactions WHERE user_id IN (?, ?)", custId, stfId);
            jdbcTemplate.update("DELETE FROM TopupTransactions WHERE user_id IN (?, ?) OR processed_by_staff_id IN (?, ?)", custId, stfId, custId, stfId);
            jdbcTemplate.update("DELETE FROM TopupTransactions WHERE sepay_code IN ('999001', '999002', '999003', '999004', '999005', '999006')");
            jdbcTemplate.update("DELETE FROM Users WHERE id IN (?, ?)", custId, stfId);
        }
    }

    @Test
    void testGetSepayConfig_AllowedForCustomer() throws Exception {
        // Generate valid JWT token for Customer
        String token = jwtTokenProvider.generateAccessToken(customerUser.getId(), customerUser.getEmail());

        mockMvc.perform(get("/api/sepay/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankId").value("TPB"))
                .andExpect(jsonPath("$.accountNumber").value("68917122004"))
                .andExpect(jsonPath("$.accountName").value("TRINH VIET ANH"));
    }

    @Test
    void testGetSepayConfig_DeniedForStaff() throws Exception {
        // Generate valid JWT token for Staff
        String token = jwtTokenProvider.generateAccessToken(staffUser.getId(), staffUser.getEmail());

        mockMvc.perform(get("/api/sepay/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nhân viên và quản trị viên không được phép nạp tiền."));
    }

    @Test
    void testGetSepayConfig_NoAuth() throws Exception {
        // No Authorization header
        mockMvc.perform(get("/api/sepay/config"))
                .andExpect(status().isOk()) // Confirmed public configs accessible for guest/unauthenticated (except role is checked if authenticated)
                .andExpect(jsonPath("$.bankId").value("TPB"));
    }

    @Test
    void testHandleSepayWebhook_TC_TOPUP_001_Success() throws Exception {
        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(999001L);
        request.setTransferType("in");
        request.setTransferAmount(150000L); // 150,000 VND
        request.setContent("MMO-TOPUP-" + customerUser.getId());
        request.setReferenceCode("FT260723TEST001");

        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey " + sepayWebhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        // Verify balance updated
        User updatedUser = userRepository.findById(customerUser.getId()).orElseThrow();
        assertEquals(200000L, updatedUser.getBalanceVnd());

        // Verify transaction logged
        Optional<TopupTransaction> txOpt = topupTransactionRepository.findBySepayCode("999001");
        assertTrue(txOpt.isPresent());
        assertEquals("Success", txOpt.get().getStatus());
        assertEquals(150000L, txOpt.get().getAmountVnd());
    }

    @Test
    void testHandleSepayWebhook_TC_TOPUP_001_B_InvalidContent() throws Exception {
        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(999002L);
        request.setTransferType("in");
        request.setTransferAmount(50000L);
        request.setContent("INVALID CONTENT STR");
        request.setReferenceCode("FT260723TEST002");

        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey " + sepayWebhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Balance remains unchanged
        User updatedUser = userRepository.findById(customerUser.getId()).orElseThrow();
        assertEquals(50000L, updatedUser.getBalanceVnd());
    }

    @Test
    void testHandleSepayWebhook_TC_TOPUP_001_C_DuplicateTransaction() throws Exception {
        // Pre-save transaction to simulate duplicate
        jdbcTemplate.update("INSERT INTO TopupTransactions (sepay_code, amount_vnd, transfer_content, user_id, status, isDelete) " +
                "VALUES ('999003', 50000, ?, ?, 'Success', 0)", "MMO TOPUP " + customerUser.getId(), customerUser.getId());

        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(999003L);
        request.setTransferType("in");
        request.setTransferAmount(50000L);
        request.setContent("MMO-TOPUP-" + customerUser.getId());
        request.setReferenceCode("FT260723TEST003");

        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey " + sepayWebhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Balance remains unchanged (not added twice)
        User updatedUser = userRepository.findById(customerUser.getId()).orElseThrow();
        assertEquals(50000L, updatedUser.getBalanceVnd());
    }

    @Test
    void testHandleSepayWebhook_TC_TOPUP_001_D_InvalidAmount() throws Exception {
        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(999004L);
        request.setTransferType("in");
        request.setTransferAmount(0L); // invalid amount
        request.setContent("MMO-TOPUP-" + customerUser.getId());

        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey " + sepayWebhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHandleSepayWebhook_TC_TOPUP_001_E_LimitChecks() throws Exception {
        // Below minimum limit (5,000 VND)
        SePayWebhookRequest requestMin = new SePayWebhookRequest();
        requestMin.setId(999005L);
        requestMin.setTransferType("in");
        requestMin.setTransferAmount(5000L);
        requestMin.setContent("MMO-TOPUP-" + customerUser.getId());

        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey " + sepayWebhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMin)))
                .andExpect(status().isBadRequest());

        // Above maximum limit (60,000,000 VND)
        SePayWebhookRequest requestMax = new SePayWebhookRequest();
        requestMax.setId(999006L);
        requestMax.setTransferType("in");
        requestMax.setTransferAmount(60000000L);
        requestMax.setContent("MMO-TOPUP-" + customerUser.getId());

        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey " + sepayWebhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMax)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHandleSepayWebhook_Unauthorized() throws Exception {
        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(999007L);
        request.setTransferType("in");
        request.setTransferAmount(20000L);
        request.setContent("MMO-TOPUP-" + customerUser.getId());

        // Unauthorized call
        mockMvc.perform(post("/api/sepay/webhook")
                        .header("Authorization", "Apikey WRONG_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
