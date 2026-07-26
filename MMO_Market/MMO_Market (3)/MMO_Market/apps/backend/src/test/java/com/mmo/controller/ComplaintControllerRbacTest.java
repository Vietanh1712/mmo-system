package com.mmo.controller;

import com.mmo.feature.complaint.controller.ComplaintController;
import com.mmo.feature.complaint.service.ComplaintService;
import com.mmo.shared.dal.ChatRepository;
import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LỚP KIỂM THỬ: ComplaintControllerRbacTest
 * Nhiệm vụ: Kiểm thử kiểm soát quyền truy cập API Khiếu nại (RBAC).
 */
@WebMvcTest(controllers = ComplaintController.class)
public class ComplaintControllerRbacTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ChatRepository chatRepository;

    @MockBean
    private ComplaintRepository complaintRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Ca kiểm thử: Quản lý unassigned lỗi không có quyền truy cập (403 Forbidden)
     */
    @Test
    void list_withoutAuth_unauthorized() throws Exception {
        mvc.perform(get("/api/complaints"))
                .andExpect(status().isUnauthorized());
    }
}
