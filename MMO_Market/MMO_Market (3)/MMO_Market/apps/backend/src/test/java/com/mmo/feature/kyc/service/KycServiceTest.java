package com.mmo.feature.kyc.service;

import com.mmo.shared.dal.KycRequestRepository;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dto.KycResponseDto;
import com.mmo.shared.dto.KycReviewRequest;
import com.mmo.shared.model.IdType;
import com.mmo.shared.model.KycRequest;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.Notification;
import com.mmo.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycRequestRepository kycRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KycStorageService kycStorageService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private KycService kycService;

    private User testUser;
    private User testStaff;
    private KycRequest testKycRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("buyer@example.com")
                .fullName("Nguyen Van A")
                .address("Hanoi")
                .role("{\"role\": \"Customer\"}")
                .isDelete(false)
                .build();

        testStaff = User.builder()
                .id(2L)
                .email("staff@example.com")
                .fullName("Nguyen Staff")
                .role("{\"role\": \"Staff\"}")
                .isDelete(false)
                .build();

        testKycRequest = KycRequest.builder()
                .id(100L)
                .user(testUser)
                .activeUserId(1L)
                .idNumber("123456789")
                .idType(IdType.CCCD)
                .requestCode("KYC-ABC123XYZ456")
                .frontIdImage("front.jpg")
                .backIdImage("back.jpg")
                .selfieImage("selfie.jpg")
                .status(KycStatus.PENDING)
                .version(1)
                .isDelete(false)
                .build();
    }

    @Test
    void submitKyc_UserNotFound_ThrowsIllegalArgumentException() {
        MultipartFile front = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);
        MultipartFile selfie = mock(MultipartFile.class);

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                kycService.submitKyc(1L, "Nguyen Van A", "01/01/1990", "Hanoi", "123456789", "CCCD", front, back, selfie)
        );

        assertEquals("User không tồn tại.", exception.getMessage());
    }

    @Test
    void submitKyc_DuplicateActiveKyc_ThrowsIllegalStateException() {
        MultipartFile front = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);
        MultipartFile selfie = mock(MultipartFile.class);

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(testUser));
        when(kycRequestRepository.existsByActiveUserId(1L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                kycService.submitKyc(1L, "Nguyen Van A", "01/01/1990", "Hanoi", "123456789", "CCCD", front, back, selfie)
        );

        assertEquals("Bạn đang có một yêu cầu định danh đang chờ duyệt hoặc đã được duyệt.", exception.getMessage());
    }

    @Test
    void submitKyc_InvalidIdType_ThrowsIllegalArgumentException() {
        MultipartFile front = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);
        MultipartFile selfie = mock(MultipartFile.class);

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(testUser));
        when(kycRequestRepository.existsByActiveUserId(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                kycService.submitKyc(1L, "Nguyen Van A", "01/01/1990", "Hanoi", "123456789", "INVALID_TYPE", front, back, selfie)
        );

        assertEquals("Loại giấy tờ không hợp lệ.", exception.getMessage());
    }

    @Test
    void submitKyc_InvalidDateFormat_ThrowsIllegalArgumentException() {
        MultipartFile front = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);
        MultipartFile selfie = mock(MultipartFile.class);

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                kycService.submitKyc(1L, "Nguyen Van A", "1990/01/01", "Hanoi", "123456789", "CCCD", front, back, selfie)
        );

        assertTrue(exception.getMessage().contains("Ngày sinh không hợp lệ"));
    }

    @Test
    void submitKyc_ValidDataWithSlashDate_Success() throws IOException {
        MultipartFile front = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);
        MultipartFile selfie = mock(MultipartFile.class);

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(testUser));
        when(kycRequestRepository.existsByActiveUserId(1L)).thenReturn(false);
        when(kycStorageService.storeFile(front)).thenReturn("stored_front.jpg");
        when(kycStorageService.storeFile(back)).thenReturn("stored_back.jpg");
        when(kycStorageService.storeFile(selfie)).thenReturn("stored_selfie.jpg");
        when(kycRequestRepository.existsByRequestCode(any(String.class))).thenReturn(false);
        
        KycRequest savedRequest = KycRequest.builder()
                .id(999L)
                .user(testUser)
                .idNumber("123456789")
                .idType(IdType.CCCD)
                .requestCode("KYC-RANDOMCODE1")
                .frontIdImage("stored_front.jpg")
                .backIdImage("stored_back.jpg")
                .selfieImage("stored_selfie.jpg")
                .status(KycStatus.PENDING)
                .isDelete(false)
                .build();

        when(kycRequestRepository.save(any(KycRequest.class))).thenReturn(savedRequest);
        when(userRepository.findUsersByPermission("APPROVE_KYC")).thenReturn(Collections.singletonList(testStaff));

        KycResponseDto response = kycService.submitKyc(1L, "Nguyen Van A Updated", "25/12/1995", "Hanoi City", "123456789", "CCCD", front, back, selfie);

        assertNotNull(response);
        assertEquals("Nguyen Van A Updated", testUser.getFullName());
        assertEquals("Hanoi City", testUser.getAddress());
        assertEquals(LocalDate.of(1995, 12, 25), testUser.getDateOfBirth());
        assertEquals("PENDING", response.getStatus());

        verify(userRepository).save(testUser);
        verify(kycRequestRepository).save(any(KycRequest.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void submitKyc_ValidDataWithDashDate_Success() throws IOException {
        MultipartFile front = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);
        MultipartFile selfie = mock(MultipartFile.class);

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(testUser));
        when(kycRequestRepository.existsByActiveUserId(1L)).thenReturn(false);
        when(kycStorageService.storeFile(front)).thenReturn("stored_front.jpg");
        when(kycStorageService.storeFile(back)).thenReturn("stored_back.jpg");
        when(kycStorageService.storeFile(selfie)).thenReturn("stored_selfie.jpg");
        when(kycRequestRepository.existsByRequestCode(any(String.class))).thenReturn(false);

        KycRequest savedRequest = KycRequest.builder()
                .id(999L)
                .user(testUser)
                .idNumber("123456789")
                .idType(IdType.PASSPORT)
                .requestCode("KYC-RANDOMCODE2")
                .frontIdImage("stored_front.jpg")
                .backIdImage("stored_back.jpg")
                .selfieImage("stored_selfie.jpg")
                .status(KycStatus.PENDING)
                .isDelete(false)
                .build();

        when(kycRequestRepository.save(any(KycRequest.class))).thenReturn(savedRequest);
        when(userRepository.findUsersByPermission("APPROVE_KYC")).thenReturn(Collections.emptyList());

        KycResponseDto response = kycService.submitKyc(1L, "Nguyen Van A", "1995-12-25", "Hanoi", "123456789", "PASSPORT", front, back, selfie);

        assertNotNull(response);
        assertEquals(LocalDate.of(1995, 12, 25), testUser.getDateOfBirth());
        verify(kycRequestRepository).save(any(KycRequest.class));
    }

    @Test
    void getMyKycHistory_Success() {
        when(kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(testKycRequest));

        List<KycResponseDto> history = kycService.getMyKycHistory(1L);

        assertEquals(1, history.size());
        assertEquals("KYC-ABC123XYZ456", history.get(0).getRequestCode());
    }

    @Test
    void getKycDocument_NotAuthorized_ThrowsSecurityException() {
        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));

        assertThrows(SecurityException.class, () ->
                kycService.getKycDocument(100L, "front", 99L, false)
        );
    }

    @Test
    void getKycDocument_AuthorizedUser_Success() {
        File expectedFile = new File("front.jpg");
        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));
        when(kycStorageService.getFile("front.jpg")).thenReturn(expectedFile);

        File result = kycService.getKycDocument(100L, "front", 1L, false);

        assertNotNull(result);
        assertEquals(expectedFile, result);
    }

    @Test
    void getKycDocument_StaffUser_Success() {
        File expectedFile = new File("back.jpg");
        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));
        when(kycStorageService.getFile("back.jpg")).thenReturn(expectedFile);

        File result = kycService.getKycDocument(100L, "back", 2L, true);

        assertNotNull(result);
        assertEquals(expectedFile, result);
    }

    @Test
    void reviewKycRequest_OptimisticLockingFailure_ThrowsException() {
        KycReviewRequest reviewRequest = new KycReviewRequest();
        reviewRequest.setVersion(2); // Different from testKycRequest (1L)

        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                kycService.reviewKycRequest(100L, 2L, reviewRequest)
        );
    }

    @Test
    void reviewKycRequest_NotPendingStatus_ThrowsIllegalStateException() {
        testKycRequest.setStatus(KycStatus.APPROVED);
        KycReviewRequest reviewRequest = new KycReviewRequest();
        reviewRequest.setVersion(1);

        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                kycService.reviewKycRequest(100L, 2L, reviewRequest)
        );

        assertEquals("Chỉ có thể duyệt hồ sơ ở trạng thái PENDING.", exception.getMessage());
    }

    @Test
    void reviewKycRequest_Approved_Success() {
        KycReviewRequest reviewRequest = new KycReviewRequest();
        reviewRequest.setVersion(1);
        reviewRequest.setStatus(KycStatus.APPROVED);

        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(testStaff));
        when(kycRequestRepository.save(any(KycRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponseDto response = kycService.reviewKycRequest(100L, 2L, reviewRequest);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals(KycStatus.APPROVED, testKycRequest.getStatus());
        assertEquals(testStaff, testKycRequest.getReviewedBy());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("Yêu cầu KYC đã được phê duyệt", notifCaptor.getValue().getTitle());
        assertEquals("SUCCESS", notifCaptor.getValue().getSeverity());
    }

    @Test
    void reviewKycRequest_Rejected_Success() {
        KycReviewRequest reviewRequest = new KycReviewRequest();
        reviewRequest.setVersion(1);
        reviewRequest.setStatus(KycStatus.REJECTED);
        reviewRequest.setRejectionReason("Anh CCCD bi mo");

        when(kycRequestRepository.findByIdAndIsDeleteFalse(100L)).thenReturn(Optional.of(testKycRequest));
        when(userRepository.findByIdAndIsDeleteFalse(2L)).thenReturn(Optional.of(testStaff));
        when(kycRequestRepository.save(any(KycRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponseDto response = kycService.reviewKycRequest(100L, 2L, reviewRequest);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertEquals(KycStatus.REJECTED, testKycRequest.getStatus());
        assertNull(testKycRequest.getActiveUserId()); // Released

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("Yêu cầu KYC bị từ chối", notifCaptor.getValue().getTitle());
        assertEquals("DANGER", notifCaptor.getValue().getSeverity());
    }

    @Test
    void getKycStatistics_Success() {
        when(kycRequestRepository.countByIsDeleteFalse()).thenReturn(10L);
        when(kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.PENDING)).thenReturn(3L);
        when(kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.APPROVED)).thenReturn(5L);
        when(kycRequestRepository.countByStatusAndIsDeleteFalse(KycStatus.REJECTED)).thenReturn(2L);

        Map<String, Long> stats = kycService.getKycStatistics();

        assertEquals(10L, stats.get("total"));
        assertEquals(3L, stats.get("pending"));
        assertEquals(5L, stats.get("approved"));
        assertEquals(2L, stats.get("rejected"));
    }
}
