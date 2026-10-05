package com.atamanahmet.cinelog.service;

import com.atamanahmet.cinelog.domain.entity.User;

/**
 * Loads the authenticated User entity by id from the security context.
 * Must be called from within a {@code @Transactional} method so the returned entity stays managed.
 * Must not open its own transaction that returns a detached entity.
 */
public interface CurrentUserService {

    /**
     * Reloads the authenticated user by id.
     */
    User getCurrentUser();
}
