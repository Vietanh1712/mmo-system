package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class DemoKycController {

    // 1. Phê duyệt hồ sơ KYC
    @PostMapping("/kyc/approve/{kycId}")
    @PreAuthorize("hasAuthority('APPROVE_KYC')")
    public ResponseEntity<Map<String, Object>> approveKyc(@PathVariable String kycId, @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã phê duyệt hồ sơ KYC " + kycId + " thành công.");
        return ResponseEntity.ok(response);
    }

    // từ chối hồ sơ KYC
    @PostMapping("/kyc/reject/{kycId}")
    @PreAuthorize("hasAuthority('APPROVE_KYC')")
    public ResponseEntity<Map<String, Object>> rejectKyc(@PathVariable String kycId, @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã từ chối hồ sơ KYC " + kycId + " thành công.");
        return ResponseEntity.ok(response);
    }

    // 2. Phê duyệt yêu cầu rút tiền
    @PostMapping("/withdrawals/approve/{withdrawalId}")
    @PreAuthorize("hasAuthority('APPROVE_WITHDRAWALS')")
    public ResponseEntity<Map<String, Object>> approveWithdrawal(@PathVariable String withdrawalId, @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã phê duyệt yêu cầu rút tiền " + withdrawalId + " thành công.");
        return ResponseEntity.ok(response);
    }

    // từ chối yêu cầu rút tiền
    @PostMapping("/withdrawals/reject/{withdrawalId}")
    @PreAuthorize("hasAuthority('APPROVE_WITHDRAWALS')")
    public ResponseEntity<Map<String, Object>> rejectWithdrawal(@PathVariable String withdrawalId, @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã từ chối yêu cầu rút tiền " + withdrawalId + " thành công.");
        return ResponseEntity.ok(response);
    }
}
