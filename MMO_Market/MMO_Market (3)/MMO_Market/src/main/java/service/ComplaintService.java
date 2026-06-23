package service;

import controller.dto.ComplaintDTO;
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
}

