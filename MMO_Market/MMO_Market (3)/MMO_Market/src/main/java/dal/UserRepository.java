package dal;

import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndIsDeleteFalse(String email);
    Optional<User> findByIdAndIsDeleteFalse(Long id);
    List<User> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    Boolean existsByEmail(String email);
    Boolean existsByEmailAndIsDeleteFalse(String email);
    long countByIsDeleteFalse();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDelete = false AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("keyword") String keyword);
}
