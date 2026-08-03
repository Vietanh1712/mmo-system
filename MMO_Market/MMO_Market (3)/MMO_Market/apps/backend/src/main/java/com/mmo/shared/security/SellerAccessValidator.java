package com.mmo.shared.security;

import com.mmo.shared.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Bộ xác thực quyền truy cập dành riêng cho Người bán (Seller) hoặc Quản trị viên (Admin).
 * Kiểm tra xem người dùng hiện tại có đủ quyền để truy cập vào các tính năng Seller Portal hay không.
 */
@Component
public class SellerAccessValidator {

    /**
     * Xác thực xem người dùng hiện tại có vai trò là Seller hoặc Admin hay không.
     * Nếu không hợp lệ hoặc chưa đăng nhập, ném ra lỗi từ chối truy cập (AccessDeniedException).
     *
     * @param user Đối tượng người dùng cần kiểm tra.
     * @throws AccessDeniedException Nếu người dùng là null hoặc không chứa quyền admin/seller.
     */
    public void assertSellerOrAdmin(User user) {
        if (user == null) {
            throw new AccessDeniedException("Yêu cầu xác thực tài khoản.");
        }
        
        String roleStr = String.valueOf(user.getRole()).toLowerCase(Locale.ROOT);
        
        // Cho phép truy cập nếu vai trò chứa "admin" hoặc "seller"
        if (roleStr.contains("admin") || roleStr.contains("seller")) {
            return;
        }

        throw new AccessDeniedException("Tài khoản không có quyền truy cập.");
    }
}
