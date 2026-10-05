package com.atamanahmet.cinelog.service;

import com.atamanahmet.cinelog.domain.entity.User;

public interface RefreshTokenService {

    /**
     * Create a new refresh-token family for this login, persist the hash, return the raw token.
     */
    String issueNewFamily(User user);

    /**
     * Rotate a presented refresh token in one transaction. Detects reuse and revokes the family.
     */
    RefreshOutcome refresh(String rawToken);

    /**
     * Revoke every active token in the family of this raw refresh token. No-op if unknown.
     */
    void revokeFamilyByRawToken(String rawToken);

    /**
     * Revoke every active refresh token of the user, ending all sessions.
     */
    void revokeAllForUser(Integer userId);
}
