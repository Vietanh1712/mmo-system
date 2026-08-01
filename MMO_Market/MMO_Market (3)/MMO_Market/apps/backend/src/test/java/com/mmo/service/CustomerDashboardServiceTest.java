package com.mmo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LỚP KIỂM THỬ: CustomerDashboardServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class CustomerDashboardServiceTest {

    /**
     * Ca kiểm thử: Get dashboard non khách hàng lỗi không có quyền truy cập (403 Forbidden).
     */
    @Test
    void getDashboard_nonCustomer_forbidden() {
        // TODO: Triển khai kiểm thử cho getDashboard_nonCustomer_forbidden
    }

    /**
     * Ca kiểm thử: Get dashboard hoạt động loại trừ pending deposit and giới hạn lists.
     */
    @Test
    void getDashboard_activeExcludesPendingDeposit_andLimitsLists() {
        // TODO: Triển khai kiểm thử cho getDashboard_activeExcludesPendingDeposit_andLimitsLists
    }
}
