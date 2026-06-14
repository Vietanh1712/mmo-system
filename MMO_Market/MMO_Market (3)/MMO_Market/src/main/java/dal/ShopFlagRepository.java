package dal;

import model.ShopFlag;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopFlagRepository extends JpaRepository<ShopFlag, Long> {
    List<ShopFlag> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
}
