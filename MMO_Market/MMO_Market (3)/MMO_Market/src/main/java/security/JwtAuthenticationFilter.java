package security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dal.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            String jwt = jwtTokenProvider.extractTokenFromHeader(authHeader);

            if (jwt != null && jwtTokenProvider.validateToken(jwt) && jwtTokenProvider.isAccessToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                if (userId != null && email != null) {
                    Optional<User> userOpt = userRepository.findByIdAndIsDeleteFalse(userId);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        if (Boolean.TRUE.equals(user.getIsLocked())) {
                            log.warn("Tài khoản đang bị khóa: {}", email);
                        } else {
                            List<GrantedAuthority> authorities = new ArrayList<>();
                            String roleJson = user.getRole();
                            if (roleJson != null && !roleJson.isBlank()) {
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode root = mapper.readTree(roleJson);
                                    if (root.has("role")) {
                                        String role = root.get("role").asText();
                                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                                    }
                                } catch (Exception e) {
                                    log.error("Lỗi parse JSON role cho user: {}", email, e);
                                }
                            }

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Đã set authentication cho user: {} với quyền: {}", email, authorities);
                        }
                    } else {
                        log.warn("User ID không tồn tại hoặc đã bị xóa: {}", userId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
