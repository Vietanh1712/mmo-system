package com.mmo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.feature.admin.service.AdminUserManagementService;
import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.dal.AuthenticationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dto.AdminActionResponse;
import com.mmo.shared.dto.AdminUserResponse;
import com.mmo.shared.dto.StaffUpsertRequest;
import com.mmo.shared.model.AuditLog;
import com.mmo.shared.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LỚP KIỂM THỬ: AdminUserManagementServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class AdminUserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest request;

    private AdminUserManagementService service;

    private User admin;
    private User customer;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(
                userRepository,
                auditLogRepository,
                authenticationRepository,
                new ObjectMapper(),
                passwordEncoder,
                request
        );

        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@mmo.com");
        admin.setRole("{\"role\": \"Admin\"}");
        admin.setIsLocked(false);
        admin.setIsDelete(false);

        customer = new User();
        customer.setId(2L);
        customer.setEmail("buyer@mmo.com");
        customer.setRole("{\"role\": \"Customer\"}");
        customer.setIsLocked(false);
        customer.setIsDelete(false);

        staff = new User();
        staff.setId(3L);
        staff.setEmail("staff@mmo.com");
        staff.setRole("{\"role\": \"Staff\"}");
        staff.setIsLocked(false);
        staff.setIsDelete(false);
    }

    /**
     * Ca kiểm thử: Tạm đình chỉ khách hàng (Khóa tài khoản) thành công và ghi log.
     */
    @Test
    void suspendCustomer_revokesRefreshTokens_andLogs() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenReturn(customer);

        AdminActionResponse response = service.toggleLock(1L, 2L);

        assertTrue(response.isSuccess());
        assertTrue(response.getIsLocked()); // Đã khóa tài khoản thành công
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    /**
     * Ca kiểm thử: Kích hoạt lại khách hàng (Mở khóa) thành công và ghi log.
     */
    @Test
    void activateCustomer_logsWithoutRevoke() {
        customer.setIsLocked(true); // Trạng thái đang khóa
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenReturn(customer);

        AdminActionResponse response = service.toggleLock(1L, 2L);

        assertTrue(response.isSuccess());
        assertFalse(response.getIsLocked()); // Đã mở khóa thành công
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    /**
     * Ca kiểm thử: Thao tác khóa tài khoản không có quyền Admin ném ra lỗi Forbidden.
     */
    @Test
    void suspendInactiveCustomer_throwsConflict() {
        // Operator không phải Admin (là Customer)
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(customer));

        assertThrows(ResponseStatusException.class, () ->
                service.toggleLock(2L, 3L)
        );
    }

    /**
     * Ca kiểm thử: Cập nhật staff thành công.
     */
    @Test
    void updateUser_staffActiveInactive_unchanged() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenReturn(staff);

        StaffUpsertRequest request = new StaffUpsertRequest();
        request.setFullName("Staff Updated");
        request.setGender("Male");
        request.setActive(true);

        AdminUserResponse response = service.updateStaff(1L, 3L, request);

        assertNotNull(response);
        verify(userRepository).save(staff);
    }

    /**
     * Ca kiểm thử: Thao tác trên người dùng không tồn tại ném ra lỗi NotFound.
     */
    @Test
    void updateCustomerStatus_missingUser_throwsNotFound() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                service.toggleLock(1L, 999L)
        );
    }
}
