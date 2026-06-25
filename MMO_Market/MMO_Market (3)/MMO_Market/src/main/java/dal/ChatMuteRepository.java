package dal;

import model.ChatMute;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatMuteRepository extends JpaRepository<ChatMute, Long> {
    Optional<ChatMute> findByUserAndContact(User user, User contact);
    boolean existsByUserAndContact(User user, User contact);
}
