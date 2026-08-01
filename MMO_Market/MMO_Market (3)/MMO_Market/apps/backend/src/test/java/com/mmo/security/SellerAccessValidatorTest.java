package com.mmo.security;

import com.mmo.shared.model.User;
import com.mmo.shared.security.SellerAccessValidator;
import com.mmo.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * LỚP KIỂM THỬ: SellerAccessValidatorTest
 * Nhiệm vụ: Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
public class SellerAccessValidatorTest {

    private final SellerAccessValidator validator = new SellerAccessValidator();

    /**
     * Ca kiểm thử: Quản trị viên bypasses assignment check
     */
    @Test
    void admin_bypassesAssignmentCheck() {
        User admin = TestFixtures.user("admin@mmo.com", "Admin");
        assertDoesNotThrow(() -> validator.assertSellerOrAdmin(admin));
    }

    /**
     * Ca kiểm thử: Quản lý unassigned lỗi không có quyền truy cập (403 Forbidden)
     */
    @Test
    void staff_unassigned_forbidden() {
        User customer = TestFixtures.user("customer@mmo.com", "Customer");
        assertThrows(AccessDeniedException.class, () -> validator.assertSellerOrAdmin(customer));
    }
}
