package com.atamanahmet.cinelog.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.CurrentUserResponse;
import com.atamanahmet.cinelog.dto.LoginRequest;
import com.atamanahmet.cinelog.dto.RegisterRequest;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.AuthResponse;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.service.AuthService;
import com.atamanahmet.cinelog.service.RefreshOutcome;
import com.atamanahmet.cinelog.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final JwtCookieUtil jwtCookieUtil;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCrypt;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public ResponseEntity<String> registerUser(RegisterRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = saveUser(request.username(), request.password(), response);
            if (authResponse == null) {
                return new ResponseEntity<>("Register error.", HttpStatus.EXPECTATION_FAILED);
            }
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.ALREADY_REPORTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @Transactional
    public ResponseEntity<String> loginUser(LoginRequest request, BindingResult bindingResult,
            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        try {
            AuthResponse authResponse = authUser(request, response);
            if (authResponse == null) {
                return new ResponseEntity<>("User not found. Please register.", HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(authResponse.getUsername(), HttpStatus.OK);
        } catch (Exception e) {
            log.info("error : " + e.getLocalizedMessage());
            return new ResponseEntity<>("Authentication Failed.", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Return the logged-in username, or 401 when there is no valid session.
     */
    @Override
    public ResponseEntity<CurrentUserResponse> getCurrentUser(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Cookie cookie = jwtCookieUtil.getJWTCookie(request);
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String username = jwtUtil.extractUsername(cookie.getValue());
            if (username == null || username.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(new CurrentUserResponse(username));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Rotate the refresh cookie into a new access + refresh pair, or clear cookies on failure.
     */
    @Override
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshCookie = jwtCookieUtil.getRefreshCookie(request);
        if (refreshCookie == null || refreshCookie.getValue() == null || refreshCookie.getValue().isBlank()) {
            jwtCookieUtil.clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshOutcome outcome = refreshTokenService.refresh(refreshCookie.getValue());
        if (outcome instanceof RefreshOutcome.Success success) {
            String accessToken = jwtUtil.generateToken(success.username());
            jwtCookieUtil.addJwtCookie(response, accessToken);
            jwtCookieUtil.addRefreshCookie(response, success.newRawRefreshToken());
            return ResponseEntity.noContent().build();
        }

        jwtCookieUtil.clearAuthCookies(response);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private AuthResponse saveUser(String username, String password, HttpServletResponse response) {
        if (userRepository.findByUsername(username) == null) {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(bCrypt.encode(password));
            persist(newUser);
            String accessToken = issueSessionCookies(newUser, response);
            return new AuthResponse(newUser.getUsername(), accessToken);
        }
        return null;
    }

    private AuthResponse authUser(LoginRequest request, HttpServletResponse response) {
        User existingUser = userRepository.findByUsername(request.username());
        if (existingUser != null) {
            if (bCrypt.matches(request.password(), existingUser.getPassword())) {
                String accessToken = issueSessionCookies(existingUser, response);
                return new AuthResponse(request.username(), accessToken);
            }
        }
        return null;
    }

    /**
     * Set access and refresh cookies for a successful login or register.
     */
    private String issueSessionCookies(User user, HttpServletResponse response) {
        String accessToken = jwtUtil.generateToken(user);
        jwtCookieUtil.addJwtCookie(response, accessToken);
        String rawRefreshToken = refreshTokenService.issueNewFamily(user);
        jwtCookieUtil.addRefreshCookie(response, rawRefreshToken);
        return accessToken;
    }

    /**
     * Save the user row.
     */
    private void persist(User user) {
        userRepository.save(user);
    }
}
