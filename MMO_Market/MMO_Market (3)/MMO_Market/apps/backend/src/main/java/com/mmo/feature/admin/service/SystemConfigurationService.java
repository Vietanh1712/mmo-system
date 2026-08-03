package com.mmo.feature.admin.service;
import com.mmo.shared.model.Transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dto.CommissionsUpdateRequest;
import com.mmo.shared.dto.SystemConfigResponse;
import com.mmo.shared.dto.SystemConfigUpdateRequest;
import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.AuditLog;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemConfigurationService {

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public SystemConfigurationService(SystemConfigurationRepository systemConfigurationRepository,
                                      UserRepository userRepository,
                                      AuditLogRepository auditLogRepository,
                                      ObjectMapper objectMapper,
                                      HttpServletRequest request) {
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    private Map<String, String> getCurrentConfigMap() {
        return systemConfigurationRepository.findAll().stream().collect(Collectors.toMap(
                SystemConfiguration::getConfigKey,
                SystemConfiguration::getConfigValue,
                (v1, v2) -> v1
        ));
    }

    private String getClientIp() {
        if (request == null) {
            return "127.0.0.1";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private void checkAndAddDiff(Map<String, Object> diff, String key, Object oldValue, Object newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            diff.put(key, oldValue + " -> " + newValue);
        }
    }

    private void saveAuditLog(User operator, String action, String desc, String ipAddress, Map<String, Object> diff) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("desc", desc);
            payload.put("ipAddress", ipAddress);
            payload.put("diff", diff);
            
            String jsonDetails = objectMapper.writeValueAsString(payload);
            
            auditLogRepository.save(AuditLog.builder()
                    .userId(operator.getId())
                    .action(action)
                    .details(jsonDetails)
                    .build());
        } catch (Exception e) {
            auditLogRepository.save(AuditLog.builder()
                    .userId(operator.getId())
                    .action(action)
                    .details(desc)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public SystemConfigResponse getConfigurations(Long operatorId) {
        requireAdmin(operatorId);

        List<SystemConfiguration> configs = systemConfigurationRepository.findAll();
        Map<String, String> map = configs.stream().collect(Collectors.toMap(
                SystemConfiguration::getConfigKey,
                SystemConfiguration::getConfigValue,
                (v1, v2) -> v1
        ));

        SystemConfigResponse.SystemConfigDto sys = SystemConfigResponse.SystemConfigDto.builder()
                .appName("MMO Market System")
                .sessionTimeout(getInt(map, "SESSION_TIMEOUT_MINS", 15))
                .otpTimeout(getInt(map, "OTP_TIMEOUT_MINS", 5))
                .maxLoginRetries(getInt(map, "MAX_LOGIN_RETRIES", 5))
                .lockDurationMins(getInt(map, "LOCK_DURATION_MINS", 15))
                .escrowHoldHours(getInt(map, "ESCROW_HOLD_HOURS", 72))
                .escrowHoldHoursLevel0(getInt(map, "ESCROW_HOLD_HOURS_LEVEL_0", 168))
                .escrowHoldHoursLevel1(getInt(map, "ESCROW_HOLD_HOURS_LEVEL_1", 72))
                .escrowHoldHoursLevel2(getInt(map, "ESCROW_HOLD_HOURS_LEVEL_2", 48))
                .build();

        SystemConfigResponse.CommissionsDto comm = SystemConfigResponse.CommissionsDto.builder()
                .basePercent(getDouble(map, "DEFAULT_COMMISSION_PERCENT", 5.0))
                .withdrawalPercent(getDouble(map, "WITHDRAWAL_FEE_PERCENT", 1.5))
                .shopOpeningFee(getLong(map, "SHOP_OPENING_FEE_VND", 500000L))
                .minWithdrawLimit(getLong(map, "MIN_WITHDRAWAL_VND", 50000L))
                .maxWithdrawLimit(getLong(map, "MAX_WITHDRAWAL_VND", 50000000L))
                .minDepositLimit(getLong(map, "MIN_DEPOSIT_LIMIT_VND", 10000L))
                .maxDepositLimit(getLong(map, "MAX_DEPOSIT_LIMIT_VND", 50000000L))
                .build();

        return SystemConfigResponse.builder()
                .systemConfig(sys)
                .commissions(comm)
                .build();
    }

    @Transactional
    public void updateGeneralConfig(Long operatorId, SystemConfigUpdateRequest request) {
        User operator = requireAdmin(operatorId);

        if (request.getSessionTimeout() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian phiên không được để trống.");
        }
        if (request.getSessionTimeout() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian phiên tối thiểu phải là 5 phút.");
        }

        if (request.getOtpTimeout() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian OTP không được để trống.");
        }
        if (request.getOtpTimeout() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian OTP tối thiểu phải là 1 phút.");
        }

        if (request.getMaxLoginRetries() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lần đăng nhập sai tối đa không được để trống.");
        }
        if (request.getMaxLoginRetries() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lần đăng nhập sai tối đa phải từ 1 trở lên.");
        }

        if (request.getLockDurationMins() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian khóa tài khoản không được để trống.");
        }
        if (request.getLockDurationMins() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian khóa tài khoản tối thiểu phải là 1 phút.");
        }

        Integer lvl0 = request.getEscrowHoldHoursLevel0() != null ? request.getEscrowHoldHoursLevel0() : request.getEscrowHoldHours();
        Integer lvl1 = request.getEscrowHoldHoursLevel1() != null ? request.getEscrowHoldHoursLevel1() : request.getEscrowHoldHours();
        Integer lvl2 = request.getEscrowHoldHoursLevel2() != null ? request.getEscrowHoldHoursLevel2() : request.getEscrowHoldHours();

        if (lvl0 == null || lvl0 < 1 || lvl1 == null || lvl1 < 1 || lvl2 == null || lvl2 < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian giam tiền cho các Level Shop tối thiểu phải từ 1 giờ trở lên.");
        }

        Map<String, String> original = getCurrentConfigMap();
        Map<String, Object> diff = new HashMap<>();
        checkAndAddDiff(diff, "sessionTimeout", getInt(original, "SESSION_TIMEOUT_MINS", 15), request.getSessionTimeout());
        checkAndAddDiff(diff, "otpTimeout", getInt(original, "OTP_TIMEOUT_MINS", 5), request.getOtpTimeout());
        checkAndAddDiff(diff, "maxLoginRetries", getInt(original, "MAX_LOGIN_RETRIES", 5), request.getMaxLoginRetries());
        checkAndAddDiff(diff, "lockDurationMins", getInt(original, "LOCK_DURATION_MINS", 15), request.getLockDurationMins());
        checkAndAddDiff(diff, "escrowHoldHoursLevel0", getInt(original, "ESCROW_HOLD_HOURS_LEVEL_0", 168), lvl0);
        checkAndAddDiff(diff, "escrowHoldHoursLevel1", getInt(original, "ESCROW_HOLD_HOURS_LEVEL_1", 72), lvl1);
        checkAndAddDiff(diff, "escrowHoldHoursLevel2", getInt(original, "ESCROW_HOLD_HOURS_LEVEL_2", 48), lvl2);

        updateKey("SESSION_TIMEOUT_MINS", String.valueOf(request.getSessionTimeout()), "Thời gian phiên (phút)", operator.getId());
        updateKey("OTP_TIMEOUT_MINS", String.valueOf(request.getOtpTimeout()), "Thời gian OTP (phút)", operator.getId());
        updateKey("MAX_LOGIN_RETRIES", String.valueOf(request.getMaxLoginRetries()), "Số lần đăng nhập sai tối đa", operator.getId());
        updateKey("LOCK_DURATION_MINS", String.valueOf(request.getLockDurationMins()), "Thời gian khóa tài khoản tạm thời (phút)", operator.getId());
        updateKey("ESCROW_HOLD_HOURS", String.valueOf(lvl1), "Thời gian giam tiền mặc định (giờ)", operator.getId());
        updateKey("ESCROW_HOLD_HOURS_LEVEL_0", String.valueOf(lvl0), "Thời gian giam tiền Level 0 (giờ)", operator.getId());
        updateKey("ESCROW_HOLD_HOURS_LEVEL_1", String.valueOf(lvl1), "Thời gian giam tiền Level 1 (giờ)", operator.getId());
        updateKey("ESCROW_HOLD_HOURS_LEVEL_2", String.valueOf(lvl2), "Thời gian giam tiền Level 2 (giờ)", operator.getId());

        String details = "Đã cập nhật cấu hình hệ thống chung.";
        saveAuditLog(operator, "Config_Update", details, getClientIp(), diff);
    }

    @Transactional
    public void updateCommissionsConfig(Long operatorId, CommissionsUpdateRequest request) {
        User operator = requireAdmin(operatorId);

        if (request.getBasePercent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hoa hồng C2C không được để trống.");
        }
        if (request.getBasePercent() < 0.0 || request.getBasePercent() > 100.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hoa hồng C2C phải nằm trong khoảng từ 0% đến 100%.");
        }

        if (request.getWithdrawalPercent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phí rút tiền không được để trống.");
        }
        if (request.getWithdrawalPercent() < 0.0 || request.getWithdrawalPercent() > 100.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phí rút tiền phải nằm trong khoảng từ 0% đến 100%.");
        }

        if (request.getShopOpeningFee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phí mở shop không được để trống.");
        }
        if (request.getShopOpeningFee() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phí mở shop không được nhỏ hơn 0 VNĐ.");
        }

        if (request.getMinWithdrawLimit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức rút tối thiểu không được để trống.");
        }
        if (request.getMinWithdrawLimit() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức rút tối thiểu không được nhỏ hơn 0 VNĐ.");
        }

        if (request.getMaxWithdrawLimit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức rút tối đa không được để trống.");
        }
        if (request.getMaxWithdrawLimit() < request.getMinWithdrawLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức rút tối đa phải lớn hơn hoặc bằng hạn mức rút tối thiểu.");
        }

        if (request.getMinDepositLimit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức nạp tối thiểu không được để trống.");
        }
        if (request.getMinDepositLimit() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức nạp tối thiểu không được nhỏ hơn 0 VNĐ.");
        }

        if (request.getMaxDepositLimit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức nạp tối đa không được để trống.");
        }
        if (request.getMaxDepositLimit() < request.getMinDepositLimit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mức nạp tối đa phải lớn hơn hoặc bằng hạn mức nạp tối thiểu.");
        }

        Map<String, String> original = getCurrentConfigMap();
        Map<String, Object> diff = new HashMap<>();
        checkAndAddDiff(diff, "basePercent", getDouble(original, "DEFAULT_COMMISSION_PERCENT", 5.0), request.getBasePercent());
        checkAndAddDiff(diff, "withdrawalPercent", getDouble(original, "WITHDRAWAL_FEE_PERCENT", 1.5), request.getWithdrawalPercent());
        checkAndAddDiff(diff, "shopOpeningFee", getLong(original, "SHOP_OPENING_FEE_VND", 500000L), request.getShopOpeningFee());
        checkAndAddDiff(diff, "minWithdrawLimit", getLong(original, "MIN_WITHDRAWAL_VND", 50000L), request.getMinWithdrawLimit());
        checkAndAddDiff(diff, "maxWithdrawLimit", getLong(original, "MAX_WITHDRAWAL_VND", 50000000L), request.getMaxWithdrawLimit());
        checkAndAddDiff(diff, "minDepositLimit", getLong(original, "MIN_DEPOSIT_LIMIT_VND", 10000L), request.getMinDepositLimit());
        checkAndAddDiff(diff, "maxDepositLimit", getLong(original, "MAX_DEPOSIT_LIMIT_VND", 50000000L), request.getMaxDepositLimit());

        updateKey("DEFAULT_COMMISSION_PERCENT", String.valueOf(request.getBasePercent()), "Phần trăm hoa hồng mặc định sàn thu của Seller", operator.getId());
        updateKey("WITHDRAWAL_FEE_PERCENT", String.valueOf(request.getWithdrawalPercent()), "Phí rút tiền (%)", operator.getId());
        updateKey("SHOP_OPENING_FEE_VND", String.valueOf(request.getShopOpeningFee()), "Phí mở shop (VNĐ)", operator.getId());
        updateKey("MIN_WITHDRAWAL_VND", String.valueOf(request.getMinWithdrawLimit()), "Số tiền rút tối thiểu", operator.getId());
        updateKey("MAX_WITHDRAWAL_VND", String.valueOf(request.getMaxWithdrawLimit()), "Số tiền rút tối đa", operator.getId());
        updateKey("MIN_DEPOSIT_LIMIT_VND", String.valueOf(request.getMinDepositLimit()), "Số tiền nạp tối thiểu", operator.getId());
        updateKey("MAX_DEPOSIT_LIMIT_VND", String.valueOf(request.getMaxDepositLimit()), "Số tiền nạp tối đa", operator.getId());

        String details = "Đã cập nhật cấu hình phí & hoa hồng.";
        saveAuditLog(operator, "Config_Update", details, getClientIp(), diff);
    }

    private void updateKey(String key, String value, String desc, Long operatorId) {
        SystemConfiguration config = systemConfigurationRepository.findByConfigKey(key)
                .orElse(SystemConfiguration.builder()
                        .configKey(key)
                        .description(desc)
                        .build());
        config.setConfigValue(value);
        config.setUpdatedBy(operatorId);
        config.setUpdatedAt(LocalDateTime.now());
        systemConfigurationRepository.save(config);
    }

    private User requireAdmin(Long operatorId) {
        if (operatorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        User operator = userRepository.findByIdAndIsDeleteFalse(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        if (!"Admin".equalsIgnoreCase(normalizeRole(operator.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin mới có quyền truy cập chức năng này.");
        }
        if (Boolean.TRUE.equals(operator.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản Admin đang bị khóa.");
        }
        return operator;
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

    private Integer getInt(Map<String, String> map, String key, Integer defaultValue) {
        String val = map.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long getLong(Map<String, String> map, String key, Long defaultValue) {
        String val = map.get(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Double getDouble(Map<String, String> map, String key, Double defaultValue) {
        String val = map.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean getBool(Map<String, String> map, String key, Boolean defaultValue) {
        String val = map.get(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val) || "1".equals(val);
    }
}
