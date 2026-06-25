package service;

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
    private dal.UserRepository userRepository;

    @Mock
    private dal.AuditLogRepository auditLogRepository;

    @Mock
    private dal.AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private service.AdminUserManagementService service;

    @BeforeEach
    void setUp() {
        service = new service.AdminUserManagementService(userRepository, auditLogRepository, authenticationRepository, new ObjectMapper(), passwordEncoder);
    }

    @Test
    void toggleLockLocksActiveUserAndWritesAuditLog() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        model.User target = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = service.toggleLock(1L, 2L);

        assertTrue(response.isSuccess());
        assertTrue(response.getIsLocked());
        assertTrue(target.getIsLocked());
        verify(auditLogRepository).save(any(model.AuditLog.class));
    }

    @Test
    void toggleLockRejectsSelfLock() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class, () -> service.toggleLock(1L, 1L));
        verify(userRepository, never()).save(any(model.User.class));
    }

    @Test
    void updateRoleRequiresAdminOperator() {
        model.User staff = user(3L, "staff@mmo.com", "{\"role\": \"Staff\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(staff));

        assertThrows(ResponseStatusException.class, () -> service.updateRole(3L, 2L, "Staff"));
        verify(userRepository, never()).save(any(model.User.class));
    }

    @Test
    void updateRoleAllowsAdminToAssignStaff() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        model.User target = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = service.updateRole(1L, 2L, "Staff");

        assertTrue(response.isSuccess());
        assertEquals("Staff", response.getNewRole());
        assertEquals("{\"role\": \"Staff\"}", target.getRole());
        verify(auditLogRepository).save(any(model.AuditLog.class));
    }

    @Test
    void createStaffCreatesNewUserAndHashesPassword() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndIsDeleteFalse("newstaff@mmo.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        model.User expectedSavedStaff = model.User.builder()
                .id(10L)
                .email("newstaff@mmo.com")
                .fullName("New Staff Member")
                .role("{\"role\": \"Staff\"}")
                .isLocked(false)
                .isDelete(false)
                .isVerified(true)
                .build();
        when(userRepository.save(any(model.User.class))).thenReturn(expectedSavedStaff);

        controller.dto.StaffUpsertRequest request = new controller.dto.StaffUpsertRequest();
        request.setEmail("newstaff@mmo.com");
        request.setFullName("New Staff Member");
        request.setPassword("password123");
        request.setActive(true);
        request.setGender("Nam");

        var response = service.createStaff(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("newstaff@mmo.com", response.getEmail());
        assertEquals("Staff", response.getRole());
        verify(userRepository).save(any(model.User.class));
        verify(auditLogRepository).save(any(model.AuditLog.class));
    }

    @Test
    void createStaffRejectsDuplicateEmail() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndIsDeleteFalse("existingstaff@mmo.com")).thenReturn(true);

        controller.dto.StaffUpsertRequest request = new controller.dto.StaffUpsertRequest();
        request.setEmail("existingstaff@mmo.com");
        request.setFullName("New Staff Member");
        request.setPassword("password123");
        request.setActive(true);

        assertThrows(ResponseStatusException.class, () -> service.createStaff(1L, request));
        verify(userRepository, never()).save(any(model.User.class));
    }

    @Test
    void updateStaffUpdatesExistingStaff() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        model.User staff = user(2L, "staff@mmo.com", "{\"role\": \"Staff\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(staff));
        when(userRepository.save(staff)).thenReturn(staff);

        controller.dto.StaffUpsertRequest request = new controller.dto.StaffUpsertRequest();
        request.setFullName("Updated Staff Name");
        request.setActive(false); // Lock the account
        request.setPhone("0987654321");
        request.setGender("Nữ");

        var response = service.updateStaff(1L, 2L, request);

        assertNotNull(response);
        assertEquals("Updated Staff Name", response.getFullName());
        assertTrue(response.getIsLocked());
        assertEquals("0987654321", staff.getPhone());
        assertEquals("Nữ", staff.getGender());
        verify(userRepository).save(staff);
        verify(auditLogRepository).save(any(model.AuditLog.class));
    }

    @Test
    void updateStaffRejectsNonStaffTarget() {
        model.User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        model.User customer = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(customer));

        controller.dto.StaffUpsertRequest request = new controller.dto.StaffUpsertRequest();
        request.setFullName("Attempted Update");

        assertThrows(ResponseStatusException.class, () -> service.updateStaff(1L, 2L, request));
        verify(userRepository, never()).save(any(model.User.class));
    }

    private model.User user(Long id, String email, String role, boolean locked) {
        return model.User.builder()
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