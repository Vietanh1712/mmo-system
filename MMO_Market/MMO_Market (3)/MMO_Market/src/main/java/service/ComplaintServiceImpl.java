package service;

import controller.dto.ComplaintDTO;
import dal.ComplaintRepository;
import jakarta.transaction.Transactional;
import model.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Override
    public List<ComplaintDTO> getAllComplaints() {

        return complaintRepository
                .findAllByIsDeleteFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getTotalComplaints() {
        return complaintRepository.countByIsDeleteFalse();
    }

    @Override
    public long getInProgressComplaints() {
        return complaintRepository
                .countByStatusAndIsDeleteFalse("InProgress");
    }

    @Override
    public long getResolvedComplaints() {
        return complaintRepository
                .countByStatusAndIsDeleteFalse("Resolved");
    }

    @Override
    public long getRefusedComplaints() {
        return complaintRepository
                .countByStatusAndIsDeleteFalse("refuse");
    }

    private ComplaintDTO toDTO(Complaint c) {

        return ComplaintDTO.builder()
                .id(c.getId())
                .customerName(c.getCustomer().getFullName())
                .customerEmail(c.getCustomer().getEmail())
                .sellerName(c.getSeller().getFullName())
                .description(c.getDescription())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }

    @Override
    public Page<ComplaintDTO> getComplaints(
            int page,
            String keyword,
            String status) {

        Pageable pageable = PageRequest.of(page, 4);

        Page<Complaint> complaints;

        boolean hasKeyword =
                keyword != null && !keyword.trim().isEmpty();

        boolean hasStatus =
                status != null &&
                        !status.trim().isEmpty() &&
                        !status.equals("ALL");

//        if (hasKeyword && hasStatus) {
//
//            complaints =
//                    complaintRepository
//                            .findByDescriptionContainingIgnoreCaseAndStatusAndIsDeleteFalse(
//                                    keyword,
//                                    status,
//                                    pageable);
//
//        } else if (hasKeyword) {
//
//            complaints =
//                    complaintRepository
//                            .findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(
//                                    keyword,
//                                    pageable);
//
//        } else if (hasStatus) {
//
//            complaints =
//                    complaintRepository
//                            .findByStatusAndIsDeleteFalse(
//                                    status,
//                                    pageable);
//
//        } else {
//
//            complaints =
//                    complaintRepository
//                            .findByIsDeleteFalse(pageable);
//        }

        if (!hasKeyword) {
            keyword = null;
        }

        if (!hasStatus) {
            status = null;
        }

        complaints =
                complaintRepository.searchComplaints(
                        keyword,
                        status,
                        pageable
                );

        return complaints.map(this::toDTO);
    }

    @Override
    public List<String> getAllStatuses() {
        return complaintRepository.getAllStatuses();
    }

    @Override
    public ComplaintDTO getComplaintById(Long id) {

        Complaint complaint = complaintRepository
                .findByIdAndIsDeleteFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Complaint not found"));

        return ComplaintDTO.builder()
                .id(complaint.getId())
                .customerName(complaint.getCustomer().getFullName())
                .customerEmail(complaint.getCustomer().getEmail())
                .sellerName(complaint.getSeller().getFullName())
                .sellerEmail(complaint.getSeller().getEmail())
                .description(complaint.getDescription())
                .evidence(complaint.getEvidence())
                .resolution(complaint.getResolution())
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt())
                .amountVnd(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getAmountVnd()
                                : null
                )
                .commissionVnd(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getCommissionVnd()
                                : null
                )
                .transactionId(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getId()
                                : null)
                .build();


    }

    @Override
    @Transactional
    public void processComplaint(
            Long complaintId,
            String status,
            String resolution) {

        Complaint complaint = complaintRepository
                .findByIdAndIsDeleteFalse(complaintId)
                .orElseThrow(() ->
                        new RuntimeException("Complaint not found"));

        complaint.setStatus(status);
        complaint.setResolution(resolution);

        complaintRepository.save(complaint);
    }
}