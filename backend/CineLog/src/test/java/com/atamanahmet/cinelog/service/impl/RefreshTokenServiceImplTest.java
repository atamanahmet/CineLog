package com.atamanahmet.cinelog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.atamanahmet.cinelog.config.AuthProperties;
import com.atamanahmet.cinelog.domain.entity.RefreshToken;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.repository.RefreshTokenRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.RefreshOutcome;

@DataJpaTest
@Import(RefreshTokenServiceImpl.class)
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "auth.access-token-ttl=15m",
        "auth.refresh-token-ttl=30d",
        "auth.cookie-secure=false",
        "auth.cookie-same-site=Lax",
        "auth.allowed-origins=http://localhost",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RefreshTokenServiceImplTest {

    @Autowired
    private RefreshTokenServiceImpl refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private User user;

    @BeforeEach
    void saveUser() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
        });
        user = transactionTemplate.execute(status -> {
            User created = new User();
            created.setUsername("alice-" + System.nanoTime());
            created.setPassword("hashed-" + System.nanoTime());
            return userRepository.save(created);
        });
    }

    /**
     * Two concurrent refreshes with the same token yield exactly one Success.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRefreshYieldsExactlyOneSuccess() throws Exception {
        String raw = transactionTemplate.execute(status -> refreshTokenService.issueNewFamily(user));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        Future<RefreshOutcome> first = pool.submit(() -> {
            ready.countDown();
            start.await();
            return refreshTokenService.refresh(raw);
        });
        Future<RefreshOutcome> second = pool.submit(() -> {
            ready.countDown();
            start.await();
            return refreshTokenService.refresh(raw);
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        RefreshOutcome outcomeA = first.get(10, TimeUnit.SECONDS);
        RefreshOutcome outcomeB = second.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        if (outcomeA instanceof RefreshOutcome.Success) {
            successes.incrementAndGet();
        }
        if (outcomeB instanceof RefreshOutcome.Success) {
            successes.incrementAndGet();
        }
        assertEquals(1, successes.get());
        assertTrue(outcomeA instanceof RefreshOutcome.Success
                || outcomeA instanceof RefreshOutcome.ReuseDetected);
        assertTrue(outcomeB instanceof RefreshOutcome.Success
                || outcomeB instanceof RefreshOutcome.ReuseDetected);
    }

    /**
     * Presenting a rotated (revoked) token revokes every active token in the family.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reuseOfRotatedTokenRevokesWholeFamily() {
        String raw = transactionTemplate.execute(status -> refreshTokenService.issueNewFamily(user));
        RefreshOutcome first = refreshTokenService.refresh(raw);
        assertInstanceOf(RefreshOutcome.Success.class, first);
        String familyId = transactionTemplate.execute(status -> refreshTokenRepository
                .findByTokenHash(RefreshTokenServiceImpl.sha256Hex(raw))
                .orElseThrow()
                .getFamilyId());

        RefreshOutcome reuse = refreshTokenService.refresh(raw);
        assertInstanceOf(RefreshOutcome.ReuseDetected.class, reuse);

        List<RefreshToken> family = transactionTemplate.execute(status -> refreshTokenRepository.findByFamilyId(familyId));
        assertNotNull(family);
        assertTrue(family.stream().allMatch(token -> token.getRevokedAt() != null));
    }

    /**
     * Cleanup deletes expired rows and keeps revoked but unexpired ones.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cleanupRemovesExpiredAndKeepsUnexpiredRevoked() {
        Instant now = Instant.now();
        transactionTemplate.executeWithoutResult(status -> {
            RefreshToken expired = new RefreshToken();
            expired.setUser(userRepository.findById(user.getId()).orElseThrow());
            expired.setTokenHash("a".repeat(64));
            expired.setFamilyId("family-expired");
            expired.setExpiresAt(now.minus(Duration.ofHours(1)));
            expired.setRevokedAt(null);
            refreshTokenRepository.save(expired);

            RefreshToken revokedUnexpired = new RefreshToken();
            revokedUnexpired.setUser(userRepository.findById(user.getId()).orElseThrow());
            revokedUnexpired.setTokenHash("b".repeat(64));
            revokedUnexpired.setFamilyId("family-revoked");
            revokedUnexpired.setExpiresAt(now.plus(Duration.ofDays(1)));
            revokedUnexpired.setRevokedAt(now.minus(Duration.ofMinutes(5)));
            refreshTokenRepository.save(revokedUnexpired);
        });

        refreshTokenService.deleteExpiredTokens();

        Boolean expiredGone = transactionTemplate.execute(status -> refreshTokenRepository.findByTokenHash("a".repeat(64)).isEmpty());
        assertEquals(Boolean.TRUE, expiredGone);
        RefreshToken kept = transactionTemplate.execute(status -> refreshTokenRepository.findByTokenHash("b".repeat(64)).orElseThrow());
        assertNotNull(kept.getRevokedAt());
        assertTrue(kept.getExpiresAt().isAfter(Instant.now()));
    }

    /**
     * Logout with an already-rotated raw token still revokes the replacement in that family.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void logoutWithRotatedTokenRevokesWholeFamily() {
        String raw = transactionTemplate.execute(status -> refreshTokenService.issueNewFamily(user));
        RefreshOutcome.Success success = assertInstanceOf(
                RefreshOutcome.Success.class,
                refreshTokenService.refresh(raw));
        String newRaw = success.newRawRefreshToken();
        String familyId = transactionTemplate.execute(status -> refreshTokenRepository
                .findByTokenHash(RefreshTokenServiceImpl.sha256Hex(newRaw))
                .orElseThrow()
                .getFamilyId());

        refreshTokenService.revokeFamilyByRawToken(raw);

        List<RefreshToken> family = transactionTemplate.execute(status -> refreshTokenRepository.findByFamilyId(familyId));
        assertNotNull(family);
        assertTrue(family.stream().allMatch(token -> token.getRevokedAt() != null));
        assertNull(transactionTemplate.execute(status -> refreshTokenRepository
                .findByFamilyIdAndRevokedAtIsNull(familyId)
                .stream()
                .findFirst()
                .orElse(null)));
    }
}
