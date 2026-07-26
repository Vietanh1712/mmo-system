package com.mmo.service;

import com.mmo.feature.complaint.service.impl.ComplaintServiceImpl;
import com.mmo.feature.seller.service.ShopLevelService;
import com.mmo.shared.dal.*;
import com.mmo.shared.model.*;
import com.mmo.feature.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LỚP KIỂM THỬ: ComplaintServiceTest
 * Kiểm thử các ca tương ứng được định nghĩa trong đặc tả MMO Market.
 */
@ExtendWith(MockitoExtension.class)
public class ComplaintServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ShopFlagRepository shopFlagRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ShopLevelService shopLevelService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    private User customer;
    private User seller;
    private User staff;
    private Product product;
    private Transaction transaction;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setEmail("buyer@mmo.com");
        customer.setFullName("Buyer A");
        customer.setIsDelete(false);

        seller = new User();
        seller.setId(2L);
        seller.setEmail("seller@mmo.com");
        seller.setFullName("Seller B");
        seller.setIsDelete(false);

        staff = new User();
        staff.setId(3L);
        staff.setEmail("staff@mmo.com");
        staff.setFullName("Staff C");
        staff.setIsDelete(false);
        
        product = new Product();
        product.setId(101L);
        product.setName("Netflix Account 1 Month");
        product.setSeller(seller);
        product.setIsDelete(false);
        
        transaction = new Transaction();
        transaction.setId(501L);
        transaction.setCustomer(customer);
        transaction.setSeller(seller);
        transaction.setProduct(product);
        transaction.setAmountVnd(100000L);
        transaction.setCommissionVnd(5000L);
        transaction.setStatus("Completed");
        transaction.setIsDelete(false);
        transaction.setCreatedAt(LocalDateTime.now().minusDays(1));

        complaint = new Complaint();
        complaint.setId(701L);
        complaint.setTransaction(transaction);
        complaint.setCustomer(customer);
        complaint.setSeller(seller);
        complaint.setStatus("PENDING_REVIEW");
        complaint.setDescription("Acc bi doi pass");
        complaint.setEvidence("http://evidence.png");
        complaint.setIsDelete(false);
        complaint.setCreatedAt(LocalDateTime.now());
    }

    /**
     * Ca kiểm thử: Co approve khi không có quản lý escalate ném ra lỗi
     */
    @Test
    void coApprove_withoutstaffEscalate_throws() {
        transaction.setStatus("Disputed");
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(customer));
        when(transactionRepository.findById(501L)).thenReturn(Optional.of(transaction));

        assertThrows(IllegalArgumentException.class, () -> 
            complaintService.createComplaint(1L, 501L, "Dispute again", "http://ev.png", "Refund")
        );
    }

    /**
     * Ca kiểm thử: Co approve after quản lý escalate approves and settles
     */
    @Test
    void coApprove_afterstaffEscalate_approvesAndSettles() {
        when(complaintRepository.findById(701L)).thenReturn(Optional.of(complaint));
        when(userRepository.findByIdAndIsDeleteFalse(3L)).thenReturn(Optional.of(staff));

        complaintService.updateComplaintStatus(701L, "Rejected", "Lý do khiếu nại sai", "None", "", 3L);

        assertEquals("Rejected", complaint.getStatus());
        assertEquals("Lý do khiếu nại sai", complaint.getResolution());
        assertEquals(staff, complaint.getResolvedBy());
        verify(complaintRepository).save(complaint);
    }
}
