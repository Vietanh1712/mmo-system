package com.mmo.feature.kyc.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.dto.KycReviewRequest;

import com.mmo.shared.dto.KycResponseDto;
import com.mmo.shared.dal.KycRequestRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import com.mmo.shared.model.IdType;
import com.mmo.shared.model.KycRequest;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.User;
import com.mmo.shared.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KycService {

    @Autowired
    private KycRequestRepository kycRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycStorageService kycStorageService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 12;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public KycResponseDto submitKyc(Long userId, String fullName, String dateOfBirth, String address, String idNumber, String idTypeStr, 
                                    MultipartFile front, MultipartFile back, MultipartFile selfie) throws IOException {
        
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại."));

        user.setFullName(fullName);
        user.setAddress(address);
        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            try {
                String dob = dateOfBirth.trim();
                if (dob.contains("/")) {
                    String[] parts = dob.split("/");
                    if (parts.length == 3) {
                        user.setDateOfBirth(java.time.LocalDate.of(
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[0])
                        ));
                    }
                } else if (dob.contains("-")) {
                    String[] parts = dob.split("-");
                    if (parts.length == 3) {
                        if (parts[0].length() == 4) { // YYYY-MM-DD
                            user.setDateOfBirth(java.time.LocalDate.of(
                                Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2])
                            ));
                        } else { // DD-MM-YYYY
                            user.setDateOfBirth(java.time.LocalDate.of(
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[0])
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi parse ngày sinh: {}", dateOfBirth, e);
                throw new IllegalArgumentException("Ngày sinh không hợp lệ. Vui lòng định dạng dd/mm/yyyy.");
            }
        }
        userRepository.save(user);

        // Pre-check duplicate active KYC
        if (kycRequestRepository.existsByActiveUserId(userId)) {
            throw new IllegalStateException("Bạn đang có một yêu cầu định danh đang chờ duyệt hoặc đã được duyệt.");
        }

        IdType idType;
        try {
            idType = IdType.valueOf(idTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại giấy tờ không hợp lệ.");
        }

        // Store files
        String frontImageName = kycStorageService.storeFile(front);
        String backImageName = kycStorageService.storeFile(back);
        String selfieImageName = kycStorageService.storeFile(selfie);

        // Generate Request Code
        String requestCode = generateUniqueRequestCode();

        KycRequest request = KycRequest.builder()
                .user(user)
                .activeUserId(userId)
                .idNumber(idNumber)
                .idType(idType)
                .requestCode(requestCode)
                .frontIdImage(frontImageName)
                .backIdImage(backImageName)
                .selfieImage(selfieImageName)
                .status(KycStatus.PENDING)
                .isDelete(false)
                .build();

        KycRequest savedRequest = kycRequestRepository.save(request);

        // 1. Tạo thông báo cho Customer
        Notification customerNotif = Notification.builder()
                .userId(user.getId())
                .title("Yêu cầu KYC đã được gửi")
                .content(String.format("Yêu cầu xác minh danh tính (KYC) mã %s của bạn đã được gửi thành công và đang chờ nhân viên kiểm duyệt.", savedRequest.getRequestCode()))
                .type("KYC")
                .severity("INFO")
                .isRead(false)
                .isDelete(false)
                .targetUrl("/account/kyc")
                .build();
        notificationRepository.save(customerNotif);

        // 2. Tạo thông báo cho Staff có quyền duyệt KYC (APPROVE_KYC)
        List<User> staffAndAdmins = userRepository.findUsersByPermission("APPROVE_KYC");
        for (User staff : staffAndAdmins) {
            if (staff.getId().equals(user.getId())) {
                continue;
            }
            Notification staffNotif = Notification.builder()
                    .userId(staff.getId())
                    .title("Yêu cầu xác minh KYC mới")
                    .content(String.format("Có yêu cầu xác minh KYC mới mã %s từ %s (%s).", savedRequest.getRequestCode(), user.getFullName(), user.getEmail()))
                    .type("KYC")
                    .severity("WARNING")
                    .isRead(false)
                    .isDelete(false)
                    .targetUrl("/staff/kyc/detail?id=" + savedRequest.getId())
                    .build();
            notificationRepository.save(staffNotif);
        }

        return mapToDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<KycResponseDto> getMyKycHistory(Long userId) {
        return kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private String generateUniqueRequestCode() {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            String code = "KYC-" + generateRandomAlphanumeric(CODE_LENGTH);
            if (!kycRequestRepository.existsByRequestCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể tạo Request Code sau 3 lần thử.");
    }

    private String generateRandomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private KycResponseDto mapToDto(KycRequest request) {
        return KycResponseDto.builder()
                .id(request.getId())
                .idNumber(request.getIdNumber())
                .idType(request.getIdType().name())
                .requestCode(request.getRequestCode())
                .status(request.getStatus().name())
                .rejectionReason(request.getRejectionReason())
                .version(request.getVersion())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .fullName(request.getUser().getFullName())
                .email(request.getUser().getEmail())
                .address(request.getUser().getAddress())
                .dateOfBirth(request.getUser().getDateOfBirth() != null ? request.getUser().getDateOfBirth().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public java.io.File getKycDocument(Long kycId, String docType, Long userId, boolean isStaff) {
        KycRequest request = kycRequestRepository.findByIdAndIsDeleteFalse(kycId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy KYC Request."));

        if (!isStaff && !request.getUser().getId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập file này.");
        }

        String fileName;
        switch (docType.toLowerCase()) {
            case "front":
                fileName = request.getFrontIdImage();
                break;
            case "back":
                fileName = request.getBackIdImage();
                break;
            case "selfie":
                fileName = request.getSelfieImage();
                break;
            default:
                throw new IllegalArgumentException("Loại tài liệu không hợp lệ.");
        }

        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        return kycStorageService.getFile(fileName);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<KycResponseDto> getAllKycRequests(
            KycStatus status,
            String requestCode,
            com.mmo.shared.model.IdType idType,
            org.springframework.data.domain.Pageable pageable) {
        
        String cleanCode = (requestCode == null || requestCode.isBlank()) ? null : requestCode.trim();
        if (cleanCode != null && cleanCode.startsWith("#")) {
            cleanCode = cleanCode.substring(1).trim();
        }
        if (cleanCode != null && cleanCode.isBlank()) {
            cleanCode = null;
        }
        org.springframework.data.domain.Page<KycRequest> page = kycRequestRepository.searchKycRequests(status, cleanCode, idType, pageable);
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public KycResponseDto getKycRequestById(Long id) {
        KycRequest request = kycRequestRepository.findByIdAndIsDeleteFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy KYC Request."));
        return mapToDto(request);
    }

    @Transactional
    public KycResponseDto reviewKycRequest(Long id, Long reviewerId, com.mmo.shared.dto.KycReviewRequest reviewRequest) {
        KycRequest request = kycRequestRepository.findByIdAndIsDeleteFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy KYC Request."));

        if (!request.getVersion().equals(reviewRequest.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(KycRequest.class, id);
        }

        if (request.getStatus() != KycStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt hồ sơ ở trạng thái PENDING.");
        }

        User reviewer = userRepository.findByIdAndIsDeleteFalse(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Người duyệt không tồn tại."));

        request.setStatus(reviewRequest.getStatus());
        request.setReviewedBy(reviewer);
        request.setReviewedAt(java.time.LocalDateTime.now());

        if (reviewRequest.getStatus() == KycStatus.REJECTED) {
            request.setRejectionReason(reviewRequest.getRejectionReason());
            request.setActiveUserId(null); // Giải phóng active_user_id để user có thể gửi lại
        } else if (reviewRequest.getStatus() == KycStatus.APPROVED) {
            User user = request.getUser();
            // Không còn tự động nâng cấp role lên Seller ở bước này nữa.
            // Role Seller sẽ được cấp khi User đăng ký Shop và được duyệt.
        }

        KycRequest updated = kycRequestRepository.save(request);

        // Tạo thông báo kết quả duyệt cho Customer
        User user = updated.getUser();
        String title = "";
        String content = "";
        String severity = "INFO";
        if (updated.getStatus() == KycStatus.APPROVED) {
            title = "Yêu cầu KYC đã được phê duyệt";
            content = String.format("Yêu cầu xác minh danh tính (KYC) mã %s của bạn đã được phê duyệt thành công. Bạn đã có thể tiến hành đăng ký mở Shop bán hàng.", updated.getRequestCode());
            severity = "SUCCESS";
        } else if (updated.getStatus() == KycStatus.REJECTED) {
            title = "Yêu cầu KYC bị từ chối";
            content = String.format("Yêu cầu xác minh danh tính (KYC) mã %s của bạn đã bị từ chối. Lý do: %s", updated.getRequestCode(), updated.getRejectionReason() != null ? updated.getRejectionReason() : "Hồ sơ không hợp lệ");
            severity = "DANGER";
        }

        Notification resultNotif = Notification.builder()
                .userId(user.getId())
                .title(title)
                .content(content)
                .type("KYC")
                .severity(severity)
                .isRead(false)
                .isDelete(false)
                .targetUrl("/account/kyc")
                .build();
        notificationRepository.save(resultNotif);

        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getKycStatistics() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("total", kycRequestRepository.countByIsDeleteFalse());
        stats.put("pending", kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.PENDING));
        stats.put("approved", kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.APPROVED));
        stats.put("rejected", kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.REJECTED));
        return stats;
    }
}
