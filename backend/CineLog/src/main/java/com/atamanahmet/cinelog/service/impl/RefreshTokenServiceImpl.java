package com.atamanahmet.cinelog.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.domain.entity.RefreshToken;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.repository.RefreshTokenRepository;
import com.atamanahmet.cinelog.service.RefreshOutcome;
import com.atamanahmet.cinelog.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int RAW_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Persist a hashed refresh token for a new login family and return the raw value for the cookie.
     */
    @Override
    @Transactional
    public String issueNewFamily(User user) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256Hex(rawToken));
        refreshToken.setFamilyId(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plus(authProperties.refreshTokenTtl()));
        refreshToken.setRevokedAt(null);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Rotate in one transaction. Lookup takes a pessimistic write lock on the token row so
     * concurrent refreshes with the same token serialize; the second sees the row revoked.
     */
    @Override
    @Transactional
    public RefreshOutcome refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new RefreshOutcome.Invalid();
        }

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHashForUpdate(sha256Hex(rawToken));
        if (found.isEmpty()) {
            return new RefreshOutcome.Invalid();
        }

        RefreshToken current = found.get();
        Instant now = Instant.now();

        if (current.getExpiresAt() == null || current.getExpiresAt().isBefore(now)) {
            return new RefreshOutcome.Invalid();
        }

        if (current.getRevokedAt() != null) {
            User user = current.getUser();
            log.warn("Refresh token reuse detected userId={} familyId={}",
                    user != null ? user.getId() : null, current.getFamilyId());
            revokeFamily(current.getFamilyId(), now);
            return new RefreshOutcome.ReuseDetected();
        }

        String newRawToken = generateRawToken();
        String newTokenHash = sha256Hex(newRawToken);

        current.setRevokedAt(now);
        current.setReplacedByTokenHash(newTokenHash);

        User user = current.getUser();
        String username = user.getUsername();

        RefreshToken replacement = new RefreshToken();
        replacement.setUser(user);
        replacement.setTokenHash(newTokenHash);
        replacement.setFamilyId(current.getFamilyId());
        replacement.setExpiresAt(now.plus(authProperties.refreshTokenTtl()));
        replacement.setRevokedAt(null);
        refreshTokenRepository.save(replacement);

        return new RefreshOutcome.Success(username, newRawToken);
    }

    /**
     * Password change path: end every session of the user.
     */
    @Override
    @Transactional
    public void revokeAllForUser(Integer userId) {
        if (userId == null) {
            return;
        }
        int revoked = refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
        log.info("Revoked {} refresh tokens for user id={}", revoked, userId);
    }

    /**
     * Logout path: revoke the whole family when the cookie is present and known.
     */
    @Override
    @Transactional
    public void revokeFamilyByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHashForUpdate(sha256Hex(rawToken));
        if (found.isEmpty()) {
            return;
        }
        revokeFamily(found.get().getFamilyId(), Instant.now());
    }

    /**
     * Delete refresh tokens whose expiresAt is in the past. Keeps revoked but unexpired rows.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }

    private void revokeFamily(String familyId, Instant revokedAt) {
        List<RefreshToken> active = refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(familyId);
        for (RefreshToken token : active) {
            token.setRevokedAt(revokedAt);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
