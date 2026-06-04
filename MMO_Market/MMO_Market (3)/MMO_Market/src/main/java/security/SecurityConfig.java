package security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public API endpoints (Cổng cho Frontend gọi lên)
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/auth/register"),
                                new AntPathRequestMatcher("/api/auth/login"),
                                new AntPathRequestMatcher("/api/auth/health"),
                                new AntPathRequestMatcher("/api/auth/refresh"),
                                new AntPathRequestMatcher("/api/auth/verify-otp"),
                                new AntPathRequestMatcher("/api/auth/resend-otp"),
                                new AntPathRequestMatcher("/api/auth/forgot-password"),
                                new AntPathRequestMatcher("/api/auth/reset-password"),
                                new AntPathRequestMatcher("/api/auth/check-reset-otp")  // <-- Đã thêm dòng này!
                        ).permitAll()

                        // Public MVC endpoints (Cho phép trình duyệt tải giao diện HTML)
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/login"),
                                new AntPathRequestMatcher("/register"),
                                new AntPathRequestMatcher("/forgot-password"),
                                new AntPathRequestMatcher("/reset-password"),
                                new AntPathRequestMatcher("/verify-otp"),
                                new AntPathRequestMatcher("/profile"),
                                new AntPathRequestMatcher("/admin/users")
                        ).permitAll()

                        // Public static resources (CSS, JS, Ảnh)
                        .requestMatchers(
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/images/**")
                        ).permitAll()

                        // Protected endpoints
                        .requestMatchers(new AntPathRequestMatcher("/dashboard")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/logout")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/profile")).authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> {
                    ex.authenticationEntryPoint((request, response, authException) -> {
                        response.setContentType("application/json");
                        response.setStatus(401);
                        response.getWriter().write("{\"message\": \"Unauthorized\", \"status\": 401}");
                    });
                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setContentType("application/json");
                        response.setStatus(403);
                        response.getWriter().write("{\"message\": \"Access Denied\", \"status\": 403}");
                    });
                });

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.httpBasic(basic -> basic.disable());

        return http.build();
    }
}
