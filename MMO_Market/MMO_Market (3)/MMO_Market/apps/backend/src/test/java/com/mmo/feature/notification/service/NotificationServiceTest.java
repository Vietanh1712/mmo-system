package com.mmo.feature.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.AuditLog;
import com.mmo.shared.model.Notification;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User adminUser;
    private User staffUser;
    private User customerUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@mmomarket.com")
                .fullName("Admin User")
                .role("{\"role\": \"Admin\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        staffUser = User.builder()
                .id(2L)
                .email("staff@mmomarket.com")
                .fullName("Staff User")
                .role("{\"role\": \"Staff\"}")
                .isLocked(false)
                .isDelete(false)
                .build();

        customerUser = User.builder()
                .id(3L)
                .email("customer@gmail.com")
                .fullName("Customer User")
                .role("{\"role\": \"Customer\"}")
                .isLocked(false)
                .isDelete(false)
                .build();
    }

    @Test
    void getNotificationsReturnsPaginatedData() {
        List<Notification> list = new ArrayList<>();
        list.add(Notification.builder()
                .id(100L)
                .userId(1L)
                .title("Notice 1")
                .content("Content 1")
                .type("info")
                .createdAt(LocalDateTime.of(2026, 6, 19, 10, 0))
                .isDelete(false)
                .build());

        Page<Notification> page = new PageImpl<>(list, PageRequest.of(0, 5), 1);
        when(notificationRepository.searchNotifications(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        Map<String, Object> result = notificationService.getNotifications("Notice", "info", 0, 5);

        assertNotNull(result);
        assertEquals(0, result.get("page"));
        assertEquals(5, result.get("size"));
        assertEquals(1L, result.get("totalElements"));
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertEquals(1, content.size());
        assertEquals("Notice 1", content.get(0).get("title"));
        assertEquals("Admin User", content.get(0).get("author"));
    }

    @Test
    void createNotificationSavesToDbAndCreatesAuditLog() throws Exception {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(adminUser));
        when(objectMapper.readTree(anyString())).thenCallRealMethod();

        notificationService.createNotification(1L, "System Upgrade", "Upgrading components...", "info", false, "127.0.0.1");

        verify(notificationRepository).save(any(Notification.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void createNotificationWithMaintenanceTogglesConfig() throws Exception {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(adminUser));
        when(objectMapper.readTree(anyString())).thenCallRealMethod();
        when(systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")).thenReturn(Optional.empty());

        notificationService.createNotification(1L, "Under Maintenance", "Maintenance content", "maintenance", true, "127.0.0.1");

        verify(notificationRepository).save(any(Notification.class));
        verify(systemConfigurationRepository).save(any(SystemConfiguration.class));
        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    void createNotificationRejectsNonAdminStaff() throws Exception {
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(customerUser));
        when(objectMapper.readTree(anyString())).thenCallRealMethod();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                notificationService.createNotification(3L, "Test Title", "Test Content", "info", false, "127.0.0.1")
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void deleteNotificationSoftDeletesAndLogs() throws Exception {
        Notification notif = Notification.builder()
                .id(100L)
                .title("Notice 1")
                .content("Content 1")
                .isDelete(false)
                .build();

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(adminUser));
        when(objectMapper.readTree(anyString())).thenCallRealMethod();
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notif));

        notificationService.deleteNotification(1L, 100L, "127.0.0.1");

        assertTrue(notif.getIsDelete());
        verify(notificationRepository).save(notif);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void toggleMaintenanceUpdatesValueAndLogs() throws Exception {
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(staffUser));
        when(objectMapper.readTree(anyString())).thenCallRealMethod();
        when(systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")).thenReturn(
                Optional.of(SystemConfiguration.builder().configKey("MAINTENANCE_MODE").configValue("FALSE").build())
        );

        notificationService.toggleMaintenance(2L, true, "127.0.0.1");

        verify(systemConfigurationRepository).save(argThat(config -> "TRUE".equals(config.getConfigValue())));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getMaintenanceStatusReturnsActiveStatusAndLatestNotif() {
        when(systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")).thenReturn(
                Optional.of(SystemConfiguration.builder().configKey("MAINTENANCE_MODE").configValue("TRUE").build())
        );

        List<Notification> list = new ArrayList<>();
        list.add(Notification.builder()
                .title("Báº£o trÃ¬")
                .content("Server maintenance message")
                .type("maintenance")
                .build());
        Page<Notification> page = new PageImpl<>(list, PageRequest.of(0, 1), 1);
        when(notificationRepository.searchNotifications("maintenance", "PUBLISHED", null, null, null, PageRequest.of(0, 1))).thenReturn(page);

        Map<String, Object> result = notificationService.getMaintenanceStatus();

        assertTrue((Boolean) result.get("active"));
        assertEquals("Server maintenance message", result.get("message"));
    }

    @Test
    void getUserNotificationsReturnsSortedList() {
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(customerUser));
        
        List<Notification> personal = new ArrayList<>();
        personal.add(Notification.builder()
                .userId(3L)
                .type("ORDER")
                .title("ÄÆ¡n hÃ ng má»›i")
                .content("Báº¡n cÃ³ Ä‘Æ¡n hÃ ng má»›i")
                .createdAt(LocalDateTime.now().minusHours(1))
                .isRead(false)
                .isDelete(false)
                .build());
        when(notificationRepository.findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(3L)).thenReturn(personal);

        // Mock findAllBroadcastNotifications
        List<Notification> broadcasts = new ArrayList<>();
        broadcasts.add(Notification.builder()
                .userId(1L)
                .title("Há»‡ thá»‘ng báº£o trÃ¬")
                .content("Báº£o trÃ¬ Ä‘á»‹nh ká»³ ngÃ y mai")
                .type("maintenance")
                .createdAt(LocalDateTime.now().minusHours(2))
                .isDelete(false)
                .build());
        when(notificationRepository.findAllBroadcastNotifications()).thenReturn(broadcasts);

        List<Map<String, Object>> result = notificationService.getUserNotifications(3L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 1 personal + 1 broadcast = 2
        assertEquals(2, result.size());
        
        // Kiá»ƒm tra xem Ä‘Ã£ sáº¯p xáº¿p giáº£m dáº§n theo thá»i gian táº¡o
        LocalDateTime dtLast = null;
        for (Map<String, Object> map : result) {
            String ca = (String) map.get("createdAt");
            LocalDateTime dt = LocalDateTime.parse(ca, java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            if (dtLast != null) {
                assertTrue(dtLast.isAfter(dt) || dtLast.isEqual(dt));
            }
            dtLast = dt;
        }
    }

    @Test
    void getUserNotificationsReturnsOnlyBroadcastsForAdminAndStaff() throws Exception {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(adminUser));
        when(notificationRepository.findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(1L)).thenReturn(new ArrayList<>());

        List<Notification> broadcasts = new ArrayList<>();
        broadcasts.add(Notification.builder()
                .userId(1L)
                .title("Notice")
                .content("Broadcast notice")
                .type("maintenance")
                .createdAt(LocalDateTime.now().minusHours(1))
                .isDelete(false)
                .build());
        when(notificationRepository.findAllBroadcastNotifications()).thenReturn(broadcasts);

        List<Map<String, Object>> result = notificationService.getUserNotifications(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Notice", result.get(0).get("title"));
        verify(notificationRepository).findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(1L);
    }


    @Test
    void markAsReadUpdatesStatusSuccessfully() {
        Notification notif = Notification.builder()
                .id(500L)
                .userId(3L)
                .isRead(false)
                .build();
        when(notificationRepository.findById(500L)).thenReturn(Optional.of(notif));

        notificationService.markAsRead(3L, 500L);

        assertTrue(notif.getIsRead());
        verify(notificationRepository).save(notif);
    }

    @Test
    void markAsReadRejectsOtherUser() {
        Notification notif = Notification.builder()
                .id(500L)
                .userId(4L) // Thuá»™c vá» user khÃ¡c
                .isRead(false)
                .build();
        when(notificationRepository.findById(500L)).thenReturn(Optional.of(notif));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(3L, 500L)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsReadUpdatesAllUnread() {
        List<Notification> list = new ArrayList<>();
        list.add(Notification.builder().id(1L).userId(3L).isRead(false).build());
        list.add(Notification.builder().id(2L).userId(3L).isRead(true).build());
        list.add(Notification.builder().id(3L).userId(3L).isRead(false).build());

        when(notificationRepository.findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(3L)).thenReturn(list);

        notificationService.markAllAsRead(3L);

        assertTrue(list.stream().allMatch(Notification::getIsRead));
        verify(notificationRepository).saveAll(list);
    }
}

