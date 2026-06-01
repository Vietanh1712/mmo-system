package dal;

import model.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthenticationRepository extends JpaRepository<Authentication, Long> {
    Optional<Authentication> findByUserIdAndProvider(Long userId, String provider);
    Optional<Authentication> findByUserIdAndIsDeleteFalse(Long userId);
    Optional<Authentication> findByRefreshToken(String refreshToken);
    List<Authentication> findAllByUserIdAndIsRevokedFalse(Long userId);
}