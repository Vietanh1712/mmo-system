package service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStatusService {
    
    private final Map<Long, Long> lastActiveMap = new ConcurrentHashMap<>();
    
    // Users are considered online if they active in the last 30 seconds (standard polling frequency)
    private static final long ONLINE_THRESHOLD_MS = 30000; 

    public void updateActiveTime(Long userId) {
        if (userId != null) {
            lastActiveMap.put(userId, System.currentTimeMillis());
        }
    }

    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Long lastActive = lastActiveMap.get(userId);
        if (lastActive == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastActive) < ONLINE_THRESHOLD_MS;
    }
}
