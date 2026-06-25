package dal;

import model.TopupTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopupTransactionRepository extends JpaRepository<TopupTransaction, Long> {
    Optional<TopupTransaction> findBySepayCode(String sepayCode);
    List<TopupTransaction> findByUserIdAndIsDeleteFalse(Long userId);
}
