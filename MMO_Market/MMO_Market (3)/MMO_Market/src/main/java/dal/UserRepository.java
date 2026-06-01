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
}
