package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controller.dto.AdminActionResponse;
import controller.dto.AdminUserResponse;
import controller.dto.StaffUpsertRequest;
import dal.AuditLogRepository;
import dal.AuthenticationRepository;
import dal.UserRepository;
import model.AuditLog;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

@Service
public class AdminUserManagementService {
    private static final List<String> ASSIGNABLE_ROLES = List.of("Customer", "Seller", "Staff");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthenticationRepository authenticationRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserManagementService(UserRepository userRepository,
                                      AuditLogRepository auditLogRepository,
                                      AuthenticationRepository authenticationRepository,
                                      ObjectMapper objectMapper,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.authenticationRepository = authenticationRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(Long operatorId) {
        requireAdmin(operatorId);
        List<AdminUserResponse> users = filteredUsers(null, null, null, null, null, null);
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAccounts", users.size());
        summary.put("activeAccounts", users.stream().filter(user -> Boolean.TRUE.equals(user.getIsOnline()) && !Boolean.TRUE.equals(user.getIsLocked())).count());
        summary.put("lockedAccounts", users.stream().filter(user -> Boolean.TRUE.equals(user.getIsLocked())).count());
        summary.put("staffAccounts", users.stream().filter(user -> "Staff".equals(user.getRole())).count());
        summary.put("verifiedAccounts", users.stream().filter(user -> Boolean.TRUE.equals(user.getIsVerified())).count());
        summary.put("sellerAccounts", users.stream().filter(user -> "Seller".equals(user.getRole())).count());
        return summary;
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long operatorId, Long userId) {
        requireAdmin(operatorId);
        return toResponse(requireExistingUser(userId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUsers(Long operatorId, String email, String phone, String name, String gender, String role, String status, int page, int size) {
        requireAdmin(operatorId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 50);
        List<AdminUserResponse> users = filteredUsers(email, phone, name, gender, role, status);
        int fromIndex = Math.min(safePage * safeSize, users.size());
        int toIndex = Math.min(fromIndex + safeSize, users.size());

        Map<String, Object> result = new HashMap<>();
        result.put("content", users.subList(fromIndex, toIndex));
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", users.size());
        result.put("totalPages", (int) Math.ceil((double) users.size() / safeSize));
        return result;
    }

    private List<AdminUserResponse> filteredUsers(String email, String phone, String name, String gender, String role, String status) {
        String emailKeyword = normalize(email);
        String phoneKeyword = normalize(phone);
        String nameKeyword = normalize(name);
        String genderKeyword = normalize(gender);
        String roleFilter = normalize(role);
        String statusFilter = normalize(status);

        return userRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .filter(res -> emailKeyword == null || contains(res.getEmail(), emailKeyword))
                .filter(res -> phoneKeyword == null || contains(res.getPhone(), phoneKeyword))
                .filter(res -> nameKeyword == null || contains(res.getFullName(), nameKeyword))
                .filter(res -> genderKeyword == null || (res.getGender() != null && res.getGender().equalsIgnoreCase(genderKeyword)))
                .filter(res -> roleFilter == null || res.getRole().toLowerCase(Locale.ROOT).contains(roleFilter))
                .filter(res -> {
                    if (statusFilter == null) return true;
                    if ("active".equals(statusFilter)) {
                        return !Boolean.TRUE.equals(res.getIsLocked()) && Boolean.TRUE.equals(res.getIsOnline());
                    } else if ("locked".equals(statusFilter)) {
                        return Boolean.TRUE.equals(res.getIsLocked());
                    } else if ("inactive".equals(statusFilter)) {
                        return !Boolean.TRUE.equals(res.getIsLocked()) && !Boolean.TRUE.equals(res.getIsOnline());
                    }
                    return true;
                })
                .toList();
    }

    @Transactional
    public AdminActionResponse toggleLock(Long operatorId, Long targetUserId) {
        User operator = requireAdmin(operatorId);
        User target = requireExistingUser(targetUserId);

        if (operator.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the tu khoa tai khoan cua chinh minh.");
        }

        boolean nextLocked = !Boolean.TRUE.equals(target.getIsLocked());
        target.setIsLocked(nextLocked);
        userRepository.save(target);

        String action = nextLocked ? "LOCK_USER" : "UNLOCK_USER";
        String details = String.format("%s (%d) da %s tai khoan %s (%d)",
                displayName(operator), operator.getId(), nextLocked ? "khoa" : "mo khoa", target.getEmail(), target.getId());
        auditLogRepository.save(AuditLog.builder()
                .userId(operator.getId())
                .action(action)
                .details(details)
                .build());

        return AdminActionResponse.builder()
                .success(true)
                .message(nextLocked ? "Da khoa tai khoan thanh cong." : "Da mo khoa tai khoan thanh cong.")
                .isLocked(nextLocked)
                .build();
    }

    @Transactional
    public AdminUserResponse createStaff(Long operatorId, StaffUpsertRequest request) {
        User operator = requireAdmin(operatorId);
        validateStaffPayload(request, true);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (Boolean.TRUE.equals(userRepository.existsByEmailAndIsDeleteFalse(email))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email da ton tai trong he thong.");
        }

        User staff = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(blankToNull(request.getPhone()))
                .gender(normalizeGender(request.getGender()))
                .address(blankToNull(request.getAddress()))
                .nationalId(blankToNull(request.getNationalId()))
                .dateOfBirth(request.getDateOfBirth())
                .role(toRoleJson("Staff"))
                .shopStatus("Approved")
                .balanceVnd(0L)
                .isVerified(true)
                .isLocked(resolveLockedFromActive(request.getActive()))
                .isDelete(false)
                .build();
        User saved = userRepository.save(staff);
        audit(operator, "CREATE_STAFF", String.format("%s (%d) da tao tai khoan Staff %s (%d)",
                displayName(operator), operator.getId(), saved.getEmail(), saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateStaff(Long operatorId, Long staffId, StaffUpsertRequest request) {
        User operator = requireAdmin(operatorId);
        User staff = requireStaff(staffId);
        validateStaffUpdatePayload(request);

        staff.setFullName(request.getFullName().trim());
        staff.setPhone(blankToNull(request.getPhone()));
        staff.setGender(normalizeGender(request.getGender()));
        staff.setAddress(blankToNull(request.getAddress()));
        staff.setNationalId(blankToNull(request.getNationalId()));
        staff.setDateOfBirth(request.getDateOfBirth());
        if (request.getActive() != null) {
            staff.setIsLocked(!Boolean.TRUE.equals(request.getActive()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            staff.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        User saved = userRepository.save(staff);
        audit(operator, "UPDATE_STAFF", String.format("%s (%d) da cap nhat tai khoan Staff %s (%d)",
                displayName(operator), operator.getId(), saved.getEmail(), saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public AdminActionResponse softDeleteUser(Long operatorId, Long targetUserId) {
        User operator = requireAdmin(operatorId);
        User target = requireExistingUser(targetUserId);
        if (operator.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the xoa tai khoan dang dang nhap.");
        }
        if ("Admin".equalsIgnoreCase(normalizeRole(target.getRole()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the xoa tai khoan Admin.");
        }

        target.setIsDelete(true);
        userRepository.save(target);
        audit(operator, "SOFT_DELETE_USER", String.format("%s (%d) da xoa mem tai khoan %s (%d)",
                displayName(operator), operator.getId(), target.getEmail(), target.getId()));
        return AdminActionResponse.builder()
                .success(true)
                .message("Da xoa tai khoan khoi he thong.")
                .build();
    }

    @Transactional
    public AdminActionResponse deleteStaff(Long operatorId, Long staffId) {
        User operator = requireAdmin(operatorId);
        User staff = requireStaff(staffId);
        if (operator.getId().equals(staff.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the xoa tai khoan dang dang nhap.");
        }

        staff.setIsDelete(true);
        userRepository.save(staff);
        audit(operator, "DELETE_STAFF", String.format("%s (%d) da xoa mem tai khoan Staff %s (%d)",
                displayName(operator), operator.getId(), staff.getEmail(), staff.getId()));
        return AdminActionResponse.builder()
                .success(true)
                .message("Da xoa tai khoan Staff.")
                .build();
    }

    @Transactional
    public AdminActionResponse updateRole(Long operatorId, Long targetUserId, String targetRole) {
        User operator = requireAdmin(operatorId);
        User target = requireExistingUser(targetUserId);
        String normalizedTargetRole = validateAssignableRole(targetRole);
        String oldRole = normalizeRole(target.getRole());

        if (operator.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the tu thay doi vai tro cua chinh minh.");
        }
        if ("Admin".equalsIgnoreCase(oldRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the thay doi vai tro tai khoan Admin.");
        }

        target.setRole(toRoleJson(normalizedTargetRole));
        userRepository.save(target);

        String details = String.format("%s (%d) da doi vai tro cua %s (%d) tu %s sang %s",
                displayName(operator), operator.getId(), target.getEmail(), target.getId(), oldRole, normalizedTargetRole);
        auditLogRepository.save(AuditLog.builder()
                .userId(operator.getId())
                .action("CHANGE_USER_ROLE")
                .details(details)
                .build());

        return AdminActionResponse.builder()
                .success(true)
                .message("Cap nhat vai tro nguoi dung thanh cong.")
                .newRole(normalizedTargetRole)
                .build();
    }

    private User requireAdmin(Long operatorId) {
        User operator = requireExistingUser(operatorId);
        if (!"Admin".equalsIgnoreCase(normalizeRole(operator.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi Admin moi co quyen truy cap chuc nang nay.");
        }
        if (Boolean.TRUE.equals(operator.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan Admin dang bi khoa.");
        }
        return operator;
    }

    private User requireExistingUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap.");
        }
        return userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay nguoi dung."));
    }

    private User requireStaff(Long userId) {
        User user = requireExistingUser(userId);
        if (!"Staff".equals(normalizeRole(user.getRole()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi duoc thao tac CRUD voi tai khoan Staff.");
        }
        return user;
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(normalizeRole(user.getRole()))
                .phone(user.getPhone())
                .shopStatus(user.getShopStatus())
                .balanceVnd(user.getBalanceVnd())
                .isVerified(Boolean.TRUE.equals(user.getIsVerified()))
                .isLocked(Boolean.TRUE.equals(user.getIsLocked()))
                .isOnline(isUserOnline(user.getId()))
                .createdAt(user.getCreatedAt())
                .gender(user.getGender())
                .address(user.getAddress())
                .nationalId(user.getNationalId())
                .dateOfBirth(user.getDateOfBirth())
                .build();
    }

    private boolean resolveLockedFromActive(Boolean active) {
        if (active == null) {
            return false;
        }
        return !Boolean.TRUE.equals(active);
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return "Nam";
        }
        String value = gender.trim();
        if (value.equalsIgnoreCase("nu") || value.equalsIgnoreCase("nữ") || value.equalsIgnoreCase("female")) {
            return "Nữ";
        }
        return "Nam";
    }

    private boolean isUserOnline(Long userId) {
        return authenticationRepository.existsByUserIdAndIsRevokedFalseAndIsDeleteFalseAndCreatedAtAfter(userId, LocalDateTime.now().minusMinutes(15));
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
        } catch (Exception ignored) {
            // Existing data may already store a plain role value.
        }
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

    private String validateAssignableRole(String role) {
        String normalized = role == null ? "" : role.trim();
        return ASSIGNABLE_ROLES.stream()
                .filter(allowed -> allowed.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai tro khong hop le."));
    }

    private String toRoleJson(String role) {
        return "{\"role\": \"" + role + "\"}";
    }

    private void validateStaffPayload(StaffUpsertRequest request, boolean requirePassword) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du lieu Staff khong hop le.");
        }
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email Staff khong hop le.");
        }
        if (request.getFullName() == null || request.getFullName().trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ho ten Staff phai co it nhat 3 ky tu.");
        }
        if (requirePassword && (request.getPassword() == null || request.getPassword().length() < 6)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau Staff phai co it nhat 6 ky tu.");
        }
        if (!requirePassword && request.getPassword() != null && !request.getPassword().isBlank() && request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau Staff phai co it nhat 6 ky tu.");
        }
    }

    private void validateStaffUpdatePayload(StaffUpsertRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du lieu Staff khong hop le.");
        }
        if (request.getFullName() == null || request.getFullName().trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ho ten Staff phai co it nhat 3 ky tu.");
        }
        if (request.getPassword() != null && !request.getPassword().isBlank() && request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau Staff phai co it nhat 6 ky tu.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(User operator, String action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .userId(operator.getId())
                .action(action)
                .details(details)
                .build());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }
}
