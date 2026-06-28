package com.mmo.feature.support.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.SupportTicket;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.mmo.feature.support.service.SupportTicketService;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupportTicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupportTicketService supportTicketService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupportTicketController supportTicketController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supportTicketController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        return auth != null ? auth.getPrincipal() : null;
                    }
                })
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTicketAllowedForCustomer() throws Exception {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("customer@mmo.com")
                .fullName("Customer A")
                .role("{\"role\": \"Customer\"}")
                .isDelete(false)
                .build();
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));

        SupportTicket ticket = new SupportTicket();
        ticket.setId(10L);
        ticket.setCategory("Lá»—i náº¡p tiá»n");
        ticket.setTitle("Náº¡p tiá»n lá»—i");
        ticket.setDescription("MÃ´ táº£ lá»—i");
        ticket.setStatus("Pending");
        ticket.setUser(user);

        when(supportTicketService.createTicket(1L, "Lá»—i náº¡p tiá»n", "Náº¡p tiá»n lá»—i", "MÃ´ táº£ lá»—i")).thenReturn(ticket);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, String> body = Map.of(
                "category", "Lá»—i náº¡p tiá»n",
                "title", "Náº¡p tiá»n lá»—i",
                "description", "MÃ´ táº£ lá»—i"
        );

        // Act & Assert
        mockMvc.perform(post("/api/support-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(jsonPath("$.user.email").value("customer@mmo.com"));
    }

    @Test
    void createTicketAllowedForSeller() throws Exception {
        // Arrange
        User user = User.builder()
                .id(2L)
                .email("seller@mmo.com")
                .fullName("Seller B")
                .role("{\"role\": \"Seller\"}")
                .isDelete(false)
                .build();
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(user));

        SupportTicket ticket = new SupportTicket();
        ticket.setId(11L);
        ticket.setCategory("GÃ³p Ã½");
        ticket.setTitle("GÃ³p Ã½ UI");
        ticket.setDescription("GÃ³p Ã½");
        ticket.setStatus("Pending");
        ticket.setUser(user);

        when(supportTicketService.createTicket(2L, "GÃ³p Ã½", "GÃ³p Ã½ UI", "GÃ³p Ã½")).thenReturn(ticket);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(2L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, String> body = Map.of(
                "category", "GÃ³p Ã½",
                "title", "GÃ³p Ã½ UI",
                "description", "GÃ³p Ã½"
        );

        // Act & Assert
        mockMvc.perform(post("/api/support-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void createTicketDeniedForStaff() throws Exception {
        // Arrange
        User user = User.builder()
                .id(3L)
                .email("staff@mmo.com")
                .fullName("Staff C")
                .role("{\"role\": \"Staff\"}")
                .isDelete(false)
                .build();
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(user));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(3L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Map<String, String> body = Map.of(
                "category", "Lá»—i náº¡p tiá»n",
                "title", "Náº¡p tiá»n lá»—i",
                "description", "MÃ´ táº£ lá»—i"
        );

        // Act & Assert
        mockMvc.perform(post("/api/support-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Chá»‰ tÃ i khoáº£n Customer hoáº·c Seller má»›i Ä‘Æ°á»£c phÃ©p táº¡o ticket há»— trá»£."));
    }

    @Test
    void getUserTicketsAllowedForCustomer() throws Exception {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("customer@mmo.com")
                .fullName("Customer A")
                .role("{\"role\": \"Customer\"}")
                .isDelete(false)
                .build();
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));

        SupportTicket ticket = new SupportTicket();
        ticket.setId(10L);
        ticket.setCategory("Lá»—i náº¡p tiá»n");
        ticket.setTitle("Náº¡p tiá»n lá»—i");
        ticket.setDescription("MÃ´ táº£ lá»—i");
        ticket.setStatus("Pending");
        ticket.setUser(user);

        when(supportTicketService.getUserTickets(1L)).thenReturn(List.of(ticket));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        mockMvc.perform(get("/api/support-tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getUserTicketsDeniedForAdmin() throws Exception {
        // Arrange
        User user = User.builder()
                .id(4L)
                .email("admin@mmo.com")
                .fullName("Admin D")
                .role("{\"role\": \"Admin\"}")
                .isDelete(false)
                .build();
        when(userRepository.findByIdAndIsDeleteFalse(4L)).thenReturn(Optional.of(user));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(4L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        mockMvc.perform(get("/api/support-tickets"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Chá»‰ tÃ i khoáº£n Customer hoáº·c Seller má»›i cÃ³ lá»‹ch sá»­ ticket."));
    }
}

