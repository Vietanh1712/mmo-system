package service;

import dal.ComplaintRepository;
import dal.KycRequestRepository;
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
    private KycRequestRepository kycRequestRepository;

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
                kycRequestRepository.countByStatusAndIsDeleteFalse(model.KycStatus.PENDING);

        return StaffDashboardDTO.builder()
                .openComplaints(openComplaints)
                .pendingKyc(pendingKyc)
                .pendingWithdrawals(pendingWithdrawals)
                .shopFlags(shopFlags)
                .totalComplaints(totalComplaints)
                .build();
    }
}