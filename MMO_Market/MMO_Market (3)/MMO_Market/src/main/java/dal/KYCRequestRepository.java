package dal;

import model.KYCRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KYCRequestRepository
        extends JpaRepository<KYCRequest, Long> {

    long countByStatus(String status);

}