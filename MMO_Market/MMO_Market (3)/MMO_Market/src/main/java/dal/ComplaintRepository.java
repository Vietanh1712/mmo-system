package dal;

import model.Complaint;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.seller = :seller AND c.status = 'Open' AND c.isDelete = false")
    long countOpenComplaintsBySeller(@Param("seller") User seller);
}
