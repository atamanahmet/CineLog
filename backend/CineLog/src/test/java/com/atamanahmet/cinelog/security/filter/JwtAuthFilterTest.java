package com.atamanahmet.cinelog.security.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.security.UserDetailsServiceImpl;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-for-hmac512";

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    private JwtUtil jwtUtil;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties(
                Duration.ofHours(1),
                Duration.ofDays(30),
                false,
                "Lax",
                List.of("http://localhost"));
        jwtUtil = new JwtUtil(SECRET, authProperties);
        filter = new JwtAuthFilter(jwtUtil, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Valid JWT whose subject no longer exists continues the chain unauthenticated.
     */
    @Test
    void validTokenForUnknownUserContinuesUnauthenticated() throws Exception {
        User deleted = new User("ghost", "unused");
        String token = jwtUtil.generateToken(deleted);
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/movies");
        request.setCookies(new Cookie("jwt_token", token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verify(userDetailsService).loadUserByUsername("ghost");
    }
}
