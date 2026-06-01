package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dal.AuditLogRepository;
import dal.AuthenticationRepository;
import dal.UserRepository;
import model.AuditLog;
import model.User;
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

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUserManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(userRepository, auditLogRepository, authenticationRepository, new ObjectMapper(), passwordEncoder);
    }

    @Test
    void toggleLockLocksActiveUserAndWritesAuditLog() {
        User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        User target = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = service.toggleLock(1L, 2L);

        assertTrue(response.isSuccess());
        assertTrue(response.getIsLocked());
        assertTrue(target.getIsLocked());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void toggleLockRejectsSelfLock() {
        User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class, () -> service.toggleLock(1L, 1L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRoleRequiresAdminOperator() {
        User staff = user(3L, "staff@mmo.com", "{\"role\": \"Staff\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(staff));

        assertThrows(ResponseStatusException.class, () -> service.updateRole(3L, 2L, "Staff"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRoleAllowsAdminToAssignStaff() {
        User admin = user(1L, "admin@mmo.com", "{\"role\": \"Admin\"}", false);
        User target = user(2L, "customer@mmo.com", "{\"role\": \"Customer\"}", false);
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = service.updateRole(1L, 2L, "Staff");

        assertTrue(response.isSuccess());
        assertEquals("Staff", response.getNewRole());
        assertEquals("{\"role\": \"Staff\"}", target.getRole());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    private User user(Long id, String email, String role, boolean locked) {
        return User.builder()
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
