package com.mmo.feature.seller.controller;

import com.mmo.shared.dal.SystemConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/config")
public class PublicSystemConfigController {

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

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
