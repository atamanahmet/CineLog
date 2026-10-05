package com.atamanahmet.cinelog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.service.RefreshTokenService;
import com.atamanahmet.cinelog.service.impl.AuthServiceImpl;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Filter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({ AuthServiceImpl.class, JwtUtil.class, JwtCookieUtil.class })
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-for-hmac512",
        "auth.access-token-ttl=3600000ms",
        "auth.refresh-token-ttl=30d",
        "auth.cookie-secure=false",
        "auth.cookie-same-site=Lax",
        "auth.allowed-origins=http://localhost"
})
class AuthMeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BCryptPasswordEncoder bCrypt;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    /**
     * No cookie means logged out.
     */
    @Test
    void noCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    /**
     * A valid JWT cookie returns the username.
     */
    @Test
    void validTokenReturnsUsername() throws Exception {
        User user = new User();
        user.setUsername("alice");
        String token = jwtUtil.generateToken(user);
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("jwt_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    /**
     * A garbage JWT cookie is logged out, not a server error.
     */
    @Test
    void garbageCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("jwt_token", "not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }
}
