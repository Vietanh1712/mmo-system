package com.mmo.feature.seller.controller;

import com.mmo.shared.dal.SystemConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller công khai (Public API) để lấy các thông số cấu hình hệ thống.
 * Các thông số này có thể được hiển thị cho người dùng chưa đăng nhập hoặc đăng nhập rồi,
 * ví dụ như: Phí mở Shop, Hạn mức nạp tiền tối thiểu/tối đa.
 */
@RestController
@RequestMapping("/api/public/config")
public class PublicSystemConfigController {

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    /**
     * Lấy mức phí mở Shop (SHOP_OPENING_FEE_VND) từ cấu hình hệ thống.
     * Dùng để hiển thị ở trang đăng ký mở gian hàng.
     */
    @GetMapping("/shop-fee")
    public ResponseEntity<Map<String, Long>> getShopOpeningFee() {
        long fee = systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")
                .map(config -> {
                    try {
                        return Long.parseLong(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        return 50000L;
                    }
                })
                .orElse(50000L);
        
        Map<String, Long> response = new HashMap<>();
        response.put("shopOpeningFee", fee);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy cấu hình Hạn mức nạp tiền tối thiểu và tối đa.
     * Dùng để hiển thị gợi ý hoặc validate ở trang Nạp tiền của khách hàng.
     */
    @GetMapping("/deposit-limits")
    public ResponseEntity<Map<String, Long>> getDepositLimits() {
        long minDeposit = systemConfigurationRepository.findByConfigKey("MIN_DEPOSIT_LIMIT_VND")
                .map(config -> {
                    try { return Long.parseLong(config.getConfigValue()); }
                    catch (NumberFormatException e) { return 10000L; }
                }).orElse(10000L);

        long maxDeposit = systemConfigurationRepository.findByConfigKey("MAX_DEPOSIT_LIMIT_VND")
                .map(config -> {
                    try { return Long.parseLong(config.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000000L; }
                }).orElse(50000000L);

        Map<String, Long> response = new HashMap<>();
        response.put("minDepositLimit", minDeposit);
        response.put("maxDepositLimit", maxDeposit);
        return ResponseEntity.ok(response);
    }
}
