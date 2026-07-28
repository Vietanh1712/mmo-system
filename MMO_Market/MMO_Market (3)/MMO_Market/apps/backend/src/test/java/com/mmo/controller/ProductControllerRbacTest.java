package com.mmo.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LỚP KIỂM THỬ: ProductControllerRbacTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class ProductControllerRbacTest {

    /**
     * Ca kiểm thử: Lấy danh sách khi không có xác thực lỗi chưa đăng nhập (401 Unauthorized).
     */
    @Test
    void list_withoutAuth_unauthorized() {
        // TODO: Triển khai kiểm thử cho list_withoutAuth_unauthorized
    }
}
