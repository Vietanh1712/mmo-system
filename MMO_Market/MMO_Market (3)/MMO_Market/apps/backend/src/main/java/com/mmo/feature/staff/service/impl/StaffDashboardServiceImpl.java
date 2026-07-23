package com.mmo.feature.staff.service.impl;

import com.mmo.shared.model.KycStatus;
import com.mmo.feature.staff.service.StaffDashboardService;

import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.dal.KycRequestRepository;
import com.mmo.shared.dal.ShopFlagRepository;
import com.mmo.shared.dal.WithdrawalRepository;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dal.SellerRegistrationRepository;
import com.mmo.shared.dal.SupportTicketRepository;
import com.mmo.shared.dto.StaffDashboardDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StaffDashboardServiceImpl implements StaffDashboardService {

        @Autowired
        private ComplaintRepository complaintRepository;

        @Autowired
        private WithdrawalRepository withdrawalRepository;

        @Autowired
        private ShopFlagRepository shopFlagRepository;

        @Autowired
        private KycRequestRepository kycRequestRepository;

        @Autowired
        private TransactionRepository transactionRepository;

        @Autowired
        private SellerRegistrationRepository sellerRegistrationRepository;

        @Autowired
        private SupportTicketRepository supportTicketRepository;

        @Autowired
        private com.mmo.shared.dal.TopupTransactionRepository topupTransactionRepository;

        @Override
        public StaffDashboardDTO getDashboardData() {

                long openComplaints = complaintRepository.countByStatusesAndNotDeleted(java.util.List.of(
                                "Open", "open", "New", "new", "InProgress", "inprogress", "In_Progress", "in_progress", "Processing", "processing"
                ));

                long pendingWithdrawals = withdrawalRepository.countByStatusAndIsDeleteFalse("Pending");

                long shopFlags = shopFlagRepository.countByStatusAndIsDeleteFalse("Effect");

                long totalComplaints = complaintRepository.count();

                long pendingKyc = kycRequestRepository
                                .countByStatusAndIsDeleteFalse(com.mmo.shared.model.KycStatus.PENDING);

                long pendingTransactions = transactionRepository.countByStatusesAndNotDeleted(java.util.List.of(
                                "Pending", "pending", "Processing", "processing"
                ));

                long totalShops = sellerRegistrationRepository.countByIsDeleteFalse();

                long pendingTickets = supportTicketRepository.countByStatusAndIsDeleteFalse("Open")
                                + supportTicketRepository.countByStatusAndIsDeleteFalse("Processing");

                long pendingTopups = topupTransactionRepository.countByStatusIgnoreCase("Failed")
                                + topupTransactionRepository.countByStatusIgnoreCase("Pending");

                return StaffDashboardDTO.builder()
                                .openComplaints(openComplaints)
                                .pendingKyc(pendingKyc)
                                .pendingWithdrawals(pendingWithdrawals)
                                .shopFlags(shopFlags)
                                .totalComplaints(totalComplaints)
                                .pendingTransactions(pendingTransactions)
                                .totalShops(totalShops)
                                .pendingTickets(pendingTickets)
                                .pendingTopups(pendingTopups)
                                .build();
        }
}