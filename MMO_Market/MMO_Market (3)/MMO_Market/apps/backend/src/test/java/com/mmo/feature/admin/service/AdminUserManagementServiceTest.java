package com.mmo.feature.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

// Using fully qualified class names since packages are at the root level
@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {
    @Mock
    private com.mmo.shared.dal.UserRepository userRepository;

    @Mock
    private com.mmo.shared.dal.AuditLogRepository auditLogRepository;

    @Mock
    private com.mmo.shared.dal.AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    private com.mmo.feature.admin.service.AdminUserManagementService service;

    @BeforeEach
    void setUp() {
        service = new com.mmo.feature.admin.service.AdminUserManagementService(userRepository, auditLogRepository, authenticationRepository, new ObjectMapper(), passwordEncoder, request);
    }

    @Test
    void toggleLockLocksActiveUserAndWritesAuditLog() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        com.mmo.shared.model.User target = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = service.toggleLock(1L, 2L);

        assertTrue(response.isSuccess());
        assertTrue(response.getIsLocked());
        assertTrue(target.getIsLocked());
        verify(auditLogRepository).save(any(com.mmo.shared.model.AuditLog.class));
    }

    @Test
    void toggleLockRejectsSelfLock() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class, () -> service.toggleLock(1L, 1L));
        verify(userRepository, never()).save(any(com.mmo.shared.model.User.class));
    }

    @Test
    void updateRoleRequiresAdminOperator() {
        com.mmo.shared.model.User staff = user(3L, "staff@mmo.com", "{\"role\": \"Staff\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(staff));

        assertThrows(ResponseStatusException.class, () -> service.updateRole(3L, 2L, "Staff"));
        verify(userRepository, never()).save(any(com.mmo.shared.model.User.class));
    }

    @Test
    void updateRoleAllowsAdminToAssignStaff() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        com.mmo.shared.model.User target = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = service.updateRole(1L, 2L, "Staff");

        assertTrue(response.isSuccess());
        assertEquals("Staff", response.getNewRole());
        assertEquals("{\"role\": \"Staff\"}", target.getRole());
        verify(auditLogRepository).save(any(com.mmo.shared.model.AuditLog.class));
    }

    @Test
    void createStaffCreatesNewUserAndHashesPassword() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndIsDeleteFalse("newstaff@mmo.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        com.mmo.shared.model.User expectedSavedStaff = com.mmo.shared.model.User.builder()
                .id(10L)
                .email("newstaff@mmo.com")
                .fullName("New Staff Member")
                .role("{\"role\": \"Staff\"}")
                .isLocked(false)
                .isDelete(false)
                .isVerified(true)
                .build();
        when(userRepository.save(any(com.mmo.shared.model.User.class))).thenReturn(expectedSavedStaff);

        com.mmo.shared.dto.StaffUpsertRequest staffRequest = new com.mmo.shared.dto.StaffUpsertRequest();
        staffRequest.setEmail("newstaff@mmo.com");
        staffRequest.setFullName("New Staff Member");
        staffRequest.setPassword("password123");
        staffRequest.setActive(true);
        staffRequest.setGender("Nam");

        var response = service.createStaff(1L, staffRequest);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("newstaff@mmo.com", response.getEmail());
        assertEquals("Staff", response.getRole());
        verify(userRepository).save(any(com.mmo.shared.model.User.class));
        verify(auditLogRepository).save(any(com.mmo.shared.model.AuditLog.class));
    }

    @Test
    void createStaffRejectsDuplicateEmail() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndIsDeleteFalse("existingstaff@mmo.com")).thenReturn(true);

        com.mmo.shared.dto.StaffUpsertRequest staffRequest = new com.mmo.shared.dto.StaffUpsertRequest();
        staffRequest.setEmail("existingstaff@mmo.com");
        staffRequest.setFullName("New Staff Member");
        staffRequest.setPassword("password123");
        staffRequest.setActive(true);

        assertThrows(ResponseStatusException.class, () -> service.createStaff(1L, staffRequest));
        verify(userRepository, never()).save(any(com.mmo.shared.model.User.class));
    }

    @Test
    void updateStaffUpdatesExistingStaff() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        com.mmo.shared.model.User staff = user(2L, "staff@mmo.com", "{\"role\": \"Staff\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(staff));
        when(userRepository.save(staff)).thenReturn(staff);

        com.mmo.shared.dto.StaffUpsertRequest staffRequest = new com.mmo.shared.dto.StaffUpsertRequest();
        staffRequest.setFullName("Updated Staff Name");
        staffRequest.setActive(false); // Lock the account
        staffRequest.setPhone("0987654321");
        staffRequest.setGender("Nam");

        var response = service.updateStaff(1L, 2L, staffRequest);

        assertNotNull(response);
        assertEquals("Updated Staff Name", response.getFullName());
        assertTrue(response.getIsLocked());
        assertEquals("0987654321", staff.getPhone());
        assertEquals("Nam", staff.getGender());
        verify(userRepository).save(staff);
        verify(auditLogRepository).save(any(com.mmo.shared.model.AuditLog.class));
    }

    @Test
    void updateStaffRejectsNonStaffTarget() {
        com.mmo.shared.model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        com.mmo.shared.model.User customer = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(customer));

        com.mmo.shared.dto.StaffUpsertRequest staffRequest = new com.mmo.shared.dto.StaffUpsertRequest();
        staffRequest.setFullName("Attempted Update");

        assertThrows(ResponseStatusException.class, () -> service.updateStaff(1L, 2L, staffRequest));
        verify(userRepository, never()).save(any(com.mmo.shared.model.User.class));
    }

    private com.mmo.shared.model.User user(Long id, String email, String role, boolean locked) {
        return com.mmo.shared.model.User.builder()
                .id(id)
                .email(email)
                .fullName(email)
                .role(role)
                .isLocked(locked)
                .isDelete(false)
                .isVerified(true)
                .build();
    }
}