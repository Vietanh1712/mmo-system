package com.mmo.feature.staff.service.impl;

import com.mmo.shared.model.KycStatus;
import com.mmo.feature.staff.service.StaffDashboardService;

import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.dal.KycRequestRepository;
import com.mmo.shared.dal.ShopFlagRepository;
import com.mmo.shared.dal.WithdrawalRepository;
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

        @Override
        public StaffDashboardDTO getDashboardData() {

                long openComplaints = complaintRepository.countByStatusAndIsDeleteFalse("Open")
                                + complaintRepository.countByStatusAndIsDeleteFalse("InProgress");

                long pendingWithdrawals = withdrawalRepository.countByStatusAndIsDeleteFalse("Pending");

                long shopFlags = shopFlagRepository.countByStatusAndIsDeleteFalse("Effect");

                long totalComplaints = complaintRepository.count();

                long pendingKyc = kycRequestRepository
                                .countByStatusAndIsDeleteFalse(com.mmo.shared.model.KycStatus.PENDING);

                return StaffDashboardDTO.builder()
                                .openComplaints(openComplaints)
                                .pendingKyc(pendingKyc)
                                .pendingWithdrawals(pendingWithdrawals)
                                .shopFlags(shopFlags)
                                .totalComplaints(totalComplaints)
                                .build();
        }
}