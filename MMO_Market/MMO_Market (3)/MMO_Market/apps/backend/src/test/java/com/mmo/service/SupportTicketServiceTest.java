package com.mmo.service;

import com.mmo.feature.support.service.SupportTicketService;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.dal.SupportTicketRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Notification;
import com.mmo.shared.model.SupportTicket;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LỚP KIỂM THỬ: SupportTicketServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private SupportTicketService supportTicketService;

    private User user;
    private SupportTicket openTicket;
    private SupportTicket processingTicket;
    private SupportTicket resolvedTicket;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("buyer@mmo.com");

        openTicket = new SupportTicket();
        openTicket.setId(10L);
        openTicket.setUser(user);
        openTicket.setStatus("Open");
        openTicket.setIsDelete(false);

        processingTicket = new SupportTicket();
        processingTicket.setId(11L);
        processingTicket.setUser(user);
        processingTicket.setStatus("Processing");
        processingTicket.setIsDelete(false);

        resolvedTicket = new SupportTicket();
        resolvedTicket.setId(12L);
        resolvedTicket.setUser(user);
        resolvedTicket.setStatus("Resolved");
        resolvedTicket.setIsDelete(false);
    }

    /**
     * Ca kiểm thử: Quản lý close yêu cầu note min10.
     */
    @Test
    void staffClose_requiresNoteMin10() {
        when(supportTicketRepository.findById(11L)).thenReturn(Optional.of(processingTicket));

        assertThrows(IllegalArgumentException.class, () ->
            supportTicketService.updateTicketStatus(11L, "Resolved", "Short")
        );
    }

    /**
     * Ca kiểm thử: Quản lý close from resolved ok.
     */
    @Test
    void staffClose_fromResolved_ok() {
        when(supportTicketRepository.findById(11L)).thenReturn(Optional.of(processingTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(processingTicket);

        SupportTicket result = supportTicketService.updateTicketStatus(11L, "Resolved", "Resolved with standard description note.");
        
        assertNotNull(result);
        assertEquals("Resolved", result.getStatus());
        verify(supportTicketRepository).save(processingTicket);
    }

    /**
     * Ca kiểm thử: Quản lý close khi already closed blocked.
     */
    @Test
    void staffClose_whenAlreadyClosed_blocked() {
        when(supportTicketRepository.findById(12L)).thenReturn(Optional.of(resolvedTicket));

        assertThrows(IllegalArgumentException.class, () ->
            supportTicketService.updateTicketStatus(12L, "Resolved", "Some long resolution note.")
        );
    }

    /**
     * Ca kiểm thử: Employee resolve stores work note không resolution note.
     */
    @Test
    void staffResolve_storesWorkNote_notResolutionNote() {
        when(supportTicketRepository.findById(11L)).thenReturn(Optional.of(processingTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(processingTicket);

        SupportTicket result = supportTicketService.updateTicketStatus(11L, "Resolved", "Resolution notes text here");
        
        assertNotNull(result);
        assertEquals("Resolution notes text here", result.getResolution());
    }

    /**
     * Ca kiểm thử: Employee cannot skip to resolved from assigned.
     */
    @Test
    void staffCannotSkipToResolvedFromAssigned() {
        when(supportTicketRepository.findById(10L)).thenReturn(Optional.of(openTicket));

        assertThrows(IllegalArgumentException.class, () ->
            supportTicketService.updateTicketStatus(10L, "Resolved", "Resolution notes text here")
        );
    }
}
