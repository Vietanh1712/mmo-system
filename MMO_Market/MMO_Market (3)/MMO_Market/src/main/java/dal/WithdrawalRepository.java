package dal;

import model.User;
import model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
    Optional<Withdrawal> findByIdAndIsDeleteFalse(Long id);
    List<Withdrawal> findByStatusAndIsDeleteFalse(String status);

    @Query("SELECT w FROM Withdrawal w JOIN FETCH w.seller WHERE w.isDelete = false")
    List<Withdrawal> findAllWithSellerByIsDeleteFalse();
}
