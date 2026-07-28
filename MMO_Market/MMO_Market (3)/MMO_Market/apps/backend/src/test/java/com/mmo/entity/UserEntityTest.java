package com.mmo.entity;

import com.mmo.shared.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LỚP KIỂM THỬ: UserEntityTest
 * Kiểm thử thực thể UserEntity (Getters, Setters, Default Values, Role & Status JSON structure).
 */
public class UserEntityTest {

    /**
     * Ca kiểm thử: Kiểm tra các hàm Getter và Setter của User.
     */
    @Test
    void testUserGettersAndSetters() {
        User user = new User();
        user.setId(100L);
        user.setEmail("buyer@mmo.com");
        user.setPassword("hashedpassword");
        user.setFullName("Nguyen Van A");
        user.setGender("Male");
        user.setPhone("0987654321");
        user.setAddress("Hanoi");
        user.setBalanceVnd(500000L);
        user.setIsLocked(false);
        user.setIsDelete(false);
        user.setIsVerified(true);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        assertEquals(100L, user.getId());
        assertEquals("buyer@mmo.com", user.getEmail());
        assertEquals("hashedpassword", user.getPassword());
        assertEquals("Nguyen Van A", user.getFullName());
        assertEquals("Male", user.getGender());
        assertEquals("0987654321", user.getPhone());
        assertEquals("Hanoi", user.getAddress());
        assertEquals(500000L, user.getBalanceVnd());
        assertFalse(user.getIsLocked());
        assertFalse(user.getIsDelete());
        assertTrue(user.getIsVerified());
        assertEquals(now, user.getCreatedAt());
    }

    /**
     * Ca kiểm thử: Kiểm tra các giá trị mặc định của thực thể.
     */
    @Test
    void testDefaultValues() {
        User user = new User();
        
        // Mặc định đối tượng mới khởi tạo các trường Boolean và Long nên bằng null hoặc false tùy định nghĩa.
        assertNull(user.getId());
        assertNull(user.getEmail());
        assertNull(user.getIsLocked());
        assertNull(user.getIsDelete());
    }

    /**
     * Ca kiểm thử: Kiểm tra cấu trúc vai trò (Role) dạng JSON String.
     */
    @Test
    void testRoleEnum() {
        User user = new User();
        user.setRole("{\"role\": \"Seller\"}");
        
        assertNotNull(user.getRole());
        assertTrue(user.getRole().contains("Seller"));
    }

    /**
     * Ca kiểm thử: Kiểm tra trạng thái của gian hàng (Shop Status).
     */
    @Test
    void testStatusEnum() {
        User user = new User();
        user.setShopStatus("Active");
        
        assertEquals("Active", user.getShopStatus());
    }
}
