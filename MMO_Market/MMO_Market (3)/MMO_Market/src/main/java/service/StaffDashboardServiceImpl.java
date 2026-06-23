package service;

import dal.ComplaintRepository;
import dal.KYCRequestRepository;
import dal.ShopFlagRepository;
import dal.WithdrawalRepository;
import controller.dto.StaffDashboardDTO;
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
    private KYCRequestRepository kycRequestRepository;

    @Override
    public StaffDashboardDTO getDashboardData() {

        long openComplaints =
                complaintRepository.countByStatusAndIsDeleteFalse("InProgress");

        long pendingWithdrawals =
                withdrawalRepository.countByStatusAndIsDeleteFalse("Pending");

        long shopFlags =
                shopFlagRepository.countByIsDeleteFalse();

        long totalComplaints =
                complaintRepository.count();

        long pendingKyc =
                kycRequestRepository.countByStatus("Pending");

        return StaffDashboardDTO.builder()
                .openComplaints(openComplaints)
                .pendingKyc(pendingKyc)
                .pendingWithdrawals(pendingWithdrawals)
                .shopFlags(shopFlags)
                .totalComplaints(totalComplaints)
                .build();
    }
}