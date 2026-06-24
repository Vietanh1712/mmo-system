package security;

import dal.UserRepository;
import model.User;
import model.Permission;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    Optional<User> userOpt = userRepository.findByIdWithPermissions(userId);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // 1. Thêm vai trò (Role)
                        String roleName = "Customer";
                        String rawRole = user.getRole();
                        if (rawRole != null && !rawRole.isBlank()) {
                            try {
                                JsonNode node = objectMapper.readTree(rawRole);
                                JsonNode roleNode = node.get("role");
                                if (roleNode != null && !roleNode.asText().isBlank()) {
                                    roleName = roleNode.asText();
                                } else {
                                    roleName = rawRole.replace("\"", "").trim();
                                }
                            } catch (Exception e) {
                                roleName = rawRole.replace("\"", "").trim();
                            }
                        }

                        // Chuẩn hóa tên vai trò
                        if (roleName.toLowerCase().contains("admin")) {
                            roleName = "Admin";
                        } else if (roleName.toLowerCase().contains("staff")) {
                            roleName = "Staff";
                        } else if (roleName.toLowerCase().contains("seller")) {
                            roleName = "Seller";
                        } else {
                            roleName = "Customer";
                        }

                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

                        // 2. Thêm các quyền hạn chi tiết (Permissions)
                        if (user.getUserPermissions() != null) {
                            for (Permission permission : user.getUserPermissions()) {
                                authorities.add(new SimpleGrantedAuthority(permission.getName()));
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Đã set authentication cho user: {} với quyền: {}", email, authorities);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
