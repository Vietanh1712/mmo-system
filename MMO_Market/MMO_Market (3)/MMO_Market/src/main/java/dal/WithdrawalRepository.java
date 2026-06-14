package dal;

import model.User;
import model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
    Optional<Withdrawal> findByIdAndIsDeleteFalse(Long id);
}
