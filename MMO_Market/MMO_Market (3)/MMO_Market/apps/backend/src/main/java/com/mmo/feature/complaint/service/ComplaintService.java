package com.mmo.feature.complaint.service;

import com.mmo.shared.dto.ComplaintDTO;
import com.mmo.shared.model.Complaint;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ComplaintService {

    List<ComplaintDTO> getAllComplaints();

    long getTotalComplaints();

    long getInProgressComplaints();

    long getResolvedComplaints();

    long getRefusedComplaints();

    Page<ComplaintDTO> getComplaints(
            int page,
            String keyword,
            String status);

    List<String> getAllStatuses();

    ComplaintDTO getComplaintById(Long id);

    void processComplaint(
            Long complaintId,
            String status,
            String resolution);
            
    // --- Methods from HEAD ---
    Complaint createComplaint(Long customerId, Long transactionId, String description, String evidence, String preferredSolution);
    
    Complaint startDispute(Long complaintId, Long staffId);
    
    List<Complaint> getCustomerComplaints(Long customerId);
    
    Complaint getComplaintById(Long complaintId, Long customerId);
    
    List<Complaint> getAllComplaintsForStaff();
    
    Complaint getComplaintByIdForStaff(Long complaintId);
    
    Complaint updateComplaintStatus(Long complaintId, String status, String resolution, String flagLevel, String flagReason, Long staffId);

    org.springframework.data.domain.Page<Complaint> searchComplaintsForStaff(String keyword, String status, int page, int size);
}
