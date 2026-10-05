package com.atamanahmet.cinelog.service.impl;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.RecommendationPreferences;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.AccountResponse;
import com.atamanahmet.cinelog.dto.ChangeEmailRequest;
import com.atamanahmet.cinelog.dto.ChangePasswordRequest;
import com.atamanahmet.cinelog.dto.RecommendationSettingsDto;
import com.atamanahmet.cinelog.exception.EmailAlreadyInUseException;
import com.atamanahmet.cinelog.exception.InvalidCurrentPasswordException;
import com.atamanahmet.cinelog.exception.PasswordUnchangedException;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.service.AccountService;
import com.atamanahmet.cinelog.service.CurrentUserService;
import com.atamanahmet.cinelog.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCrypt;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final JwtCookieUtil jwtCookieUtil;
    private final RecommendationLimitsProperties recommendationLimitsProperties;

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount() {
        return toAccountResponse(currentUserService.getCurrentUser());
    }

    @Override
    @Transactional
    public AccountResponse changeEmail(ChangeEmailRequest request) {
        User user = currentUserService.getCurrentUser();
        verifyCurrentPassword(user, request.currentPassword());

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (email.equals(user.getEmail())) {
            return toAccountResponse(user);
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new EmailAlreadyInUseException();
        }

        user.setEmail(email);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyInUseException();
        }
        log.info("Email changed for user id={}", user.getId());
        return toAccountResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletResponse response) {
        User user = currentUserService.getCurrentUser();
        verifyCurrentPassword(user, request.currentPassword());
        if (bCrypt.matches(request.newPassword(), user.getPassword())) {
            throw new PasswordUnchangedException();
        }

        user.setPassword(bCrypt.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());
        jwtCookieUtil.addJwtCookie(response, jwtUtil.generateToken(user));
        jwtCookieUtil.addRefreshCookie(response, refreshTokenService.issueNewFamily(user));
        log.info("Password changed for user id={}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationSettingsDto getRecommendationSettings() {
        return toSettingsDto(preferencesOf(currentUserService.getCurrentUser()));
    }

    @Override
    @Transactional
    public RecommendationSettingsDto updateRecommendationSettings(RecommendationSettingsDto settings) {
        User user = currentUserService.getCurrentUser();
        user.setRecommendationPreferences(new RecommendationPreferences(
                settings.scope(),
                settings.minScore(),
                settings.maxResults()));
        userRepository.save(user);
        return toSettingsDto(preferencesOf(user));
    }

    private void verifyCurrentPassword(User user, String rawPassword) {
        if (rawPassword == null || !bCrypt.matches(rawPassword, user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }
    }

    private static RecommendationPreferences preferencesOf(User user) {
        RecommendationPreferences prefs = user.getRecommendationPreferences();
        return prefs == null ? RecommendationPreferences.defaults() : prefs.withDefaults();
    }

    private static AccountResponse toAccountResponse(User user) {
        return new AccountResponse(user.getUsername(), user.getEmail());
    }

    private RecommendationSettingsDto toSettingsDto(RecommendationPreferences prefs) {
        return new RecommendationSettingsDto(
                prefs.getScope(),
                prefs.getMinScore(),
                prefs.getMaxResults(),
                recommendationLimitsProperties.getMaxResults());
    }
}
