package com.atamanahmet.cinelog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.RegisterRequest;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtCookieUtil jwtCookieUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCrypt;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthServiceImpl authService;

    /**
     * Register saves a new user with null id, username, and hashed password.
     */
    @Test
    void registerSavesUserWithNullIdAndDefaultLists() {
        when(userRepository.findByUsername("alice")).thenReturn(null);
        when(bCrypt.encode("N7k#mP2wQx!")).thenReturn("hashed");
        when(jwtUtil.generateToken(any(User.class))).thenReturn("token");
        when(refreshTokenService.issueNewFamily(any(User.class))).thenReturn("raw-refresh");

        authService.registerUser(new RegisterRequest("alice", "N7k#mP2wQx!"), response);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals("alice", saved.getUsername());
        assertEquals("hashed", saved.getPassword());
        assertTrue(saved.getRecommendation() == null || saved.getRecommendation().isEmpty());
    }
}
