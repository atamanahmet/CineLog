package com.atamanahmet.cinelog.service;

/**
 * Outcome of a refresh-token rotation attempt.
 */
public sealed interface RefreshOutcome {

    /**
     * Rotation succeeded. Carries plain values only; no managed entity.
     */
    record Success(String username, String newRawRefreshToken) implements RefreshOutcome {
    }

    /**
     * Token missing, unknown, or past expiresAt.
     */
    record Invalid() implements RefreshOutcome {
    }

    /**
     * Already-revoked token presented again. Family was revoked.
     */
    record ReuseDetected() implements RefreshOutcome {
    }
}
