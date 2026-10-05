package com.atamanahmet.cinelog.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.atamanahmet.cinelog.exception.UnauthorizedActionException;

class UserUtilTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Valid principal yields the authenticated user id.
     */
    @Test
    void getCurrentUserIdReturnsPrincipalId() {
        UserDetailsImpl details = new UserDetailsImpl(7, "alice", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        assertEquals(7, UserUtil.getCurrentUserId());
    }

    /**
     * Valid principal yields the authenticated username.
     */
    @Test
    void getCurrentUserUsernameReturnsPrincipalUsername() {
        UserDetailsImpl details = new UserDetailsImpl(7, "alice", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        assertEquals("alice", UserUtil.getCurrentUserUsername());
    }

    /**
     * AnonymousAuthenticationToken is rejected with UnauthorizedActionException.
     */
    @Test
    void rejectsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThrows(UnauthorizedActionException.class, UserUtil::getCurrentUserId);
        assertThrows(UnauthorizedActionException.class, UserUtil::getCurrentUserUsername);
    }

    /**
     * Wrong principal type is rejected with UnauthorizedActionException.
     */
    @Test
    void rejectsWrongPrincipalType() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-user-details", null, List.of()));

        assertThrows(UnauthorizedActionException.class, UserUtil::getCurrentUserId);
        assertThrows(UnauthorizedActionException.class, UserUtil::getCurrentUserUsername);
    }
}
