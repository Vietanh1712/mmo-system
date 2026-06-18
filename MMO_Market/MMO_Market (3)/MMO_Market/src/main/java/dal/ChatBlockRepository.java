package dal;

import model.ChatBlock;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatBlockRepository extends JpaRepository<ChatBlock, Long> {
    Optional<ChatBlock> findByBlockerAndBlocked(User blocker, User blocked);
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
}
