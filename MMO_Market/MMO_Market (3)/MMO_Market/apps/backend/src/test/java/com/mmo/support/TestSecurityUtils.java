package com.mmo.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

public final class TestSecurityUtils {

    private TestSecurityUtils() {}

    public static RequestPostProcessor userPrincipal(Long userId, String role) {
        String roleName = "ROLE_" + role.toUpperCase();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId,
                "N/A",
                List.of(new SimpleGrantedAuthority(roleName)));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }
}
