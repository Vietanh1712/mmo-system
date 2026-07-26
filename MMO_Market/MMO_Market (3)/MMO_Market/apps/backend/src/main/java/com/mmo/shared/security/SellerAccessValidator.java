package com.mmo.shared.security;

import com.mmo.shared.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SellerAccessValidator {

    public void assertSellerOrAdmin(User user) {
        if (user == null) {
            throw new AccessDeniedException("Yêu cầu xác thực tài khoản.");
        }
        
        String roleStr = String.valueOf(user.getRole()).toLowerCase(Locale.ROOT);
        
        // Admin or Seller roles are authorized
        if (roleStr.contains("admin") || roleStr.contains("seller")) {
            return;
        }

        throw new AccessDeniedException("Tài khoản không có quyền truy cập.");
    }
}
