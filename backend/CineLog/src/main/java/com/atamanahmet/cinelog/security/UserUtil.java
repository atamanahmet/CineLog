package com.atamanahmet.cinelog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.atamanahmet.cinelog.exception.UnauthorizedActionException;

/**
 * Pure SecurityContext reader for the authenticated principal.
 */
public final class UserUtil {

    private UserUtil() {
    }

    /**
     * Returns the current SecurityContext authentication.
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Returns the authenticated user id.
     */
    public static Integer getCurrentUserId() {
        return requirePrincipal().getUserId();
    }

    /**
     * Returns the authenticated username.
     */
    public static String getCurrentUserUsername() {
        return requirePrincipal().getUsername();
    }

    private static UserDetailsImpl requirePrincipal() {
        Authentication authentication = getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new UnauthorizedActionException("Authentication needed for this action");
        }
        return principal;
    }
}
