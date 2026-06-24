package service;

import controller.dto.KycResponseDto;
import dal.KycRequestRepository;
import dal.UserRepository;
import lombok.extern.slf4j.Slf4j;
import model.IdType;
import model.KycRequest;
import model.KycStatus;
import model.User;
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
                // Parse format DD/MM/YYYY
                String[] parts = dateOfBirth.split("/");
                if (parts.length == 3) {
                    user.setDateOfBirth(java.time.LocalDate.of(
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[0])
                    ));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
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
    public org.springframework.data.domain.Page<KycResponseDto> getAllKycRequests(KycStatus status, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<KycRequest> page;
        if (status == null) {
            page = kycRequestRepository.findAllByIsDeleteFalse(pageable);
        } else {
            page = kycRequestRepository.findByStatusAndIsDeleteFalse(status, pageable);
        }
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public KycResponseDto getKycRequestById(Long id) {
        KycRequest request = kycRequestRepository.findByIdAndIsDeleteFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy KYC Request."));
        return mapToDto(request);
    }

    @Transactional
    public KycResponseDto reviewKycRequest(Long id, Long reviewerId, controller.dto.KycReviewRequest reviewRequest) {
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
        return mapToDto(updated);
    }
}
