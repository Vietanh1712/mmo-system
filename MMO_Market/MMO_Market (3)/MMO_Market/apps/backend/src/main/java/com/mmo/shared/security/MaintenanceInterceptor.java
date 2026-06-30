package com.mmo.shared.security;
import com.mmo.shared.model.SystemConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public MaintenanceInterceptor(SystemConfigurationRepository systemConfigurationRepository,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper) {
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String val = systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")
                .map(com.mmo.shared.model.SystemConfiguration::getConfigValue)
                .orElse("FALSE");

        if (!"TRUE".equalsIgnoreCase(val)) {
            return true; // Not in maintenance mode
        }

        // Check if logged in user is Admin or Staff
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long) {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findByIdAndIsDeleteFalse(userId).orElse(null);
            if (user != null) {
                String role = normalizeRole(user.getRole());
                if ("Admin".equalsIgnoreCase(role) || "Staff".equalsIgnoreCase(role)) {
                    return true; // Admin and Staff are bypassed
                }
            }
        }

        String uri = request.getRequestURI();

        // Allow static resources, auth, admin, public search
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.equals("/error")) {
            return true;
        }
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/admin/") || uri.startsWith("/api/search/")) {
            return true;
        }
        if (uri.equals("/notifications") || uri.equals("/api/notifications")) {
            return true;
        }
        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register") || uri.equals("/support")) {
            return true;
        }

        // Block API endpoints under maintenance
        if (uri.startsWith("/api/")) {
            sendMaintenanceJsonResponse(response);
            return false;
        }

        // Block web page endpoints by redirecting to public notifications page
        response.sendRedirect("/notifications");
        return false;
    }

    private void sendMaintenanceJsonResponse(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(503); // Service Unavailable
        response.getWriter().write("{\"success\":false,\"message\":\"Hệ thống đang bảo trì để nâng cấp định kỳ. Vui lòng quay lại sau.\",\"status\":503}");
    }

    private String normalizeRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return "Customer";
        }
        try {
            JsonNode node = objectMapper.readTree(roleValue);
            JsonNode roleNode = node.get("role");
            if (roleNode != null && !roleNode.asText().isBlank()) {
                return canonicalRole(roleNode.asText());
            }
        } catch (Exception ignored) {}
        return canonicalRole(roleValue.replace("\"", "").trim());
    }

    private String canonicalRole(String role) {
        if (role == null || role.isBlank()) {
            return "Customer";
        }
        String normalized = role.trim();
        if (normalized.toLowerCase(Locale.ROOT).contains("admin")) {
            return "Admin";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("staff")) {
            return "Staff";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("seller")) {
            return "Seller";
        }
        return "Customer";
    }
}
