package com.atamanahmet.cinelog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.exception.UserNotFoundException;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.UserDetailsImpl;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserServiceImpl currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Reloads the managed user by the authenticated principal id.
     */
    @Test
    void getCurrentUserReloadsById() {
        UserDetailsImpl details = new UserDetailsImpl(7, "alice", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        User managed = new User();
        managed.setId(7);
        managed.setUsername("alice");
        when(userRepository.findById(7)).thenReturn(Optional.of(managed));

        User current = currentUserService.getCurrentUser();

        assertEquals(7, current.getId());
        assertEquals("alice", current.getUsername());
        assertEquals(managed, current);
    }

    /**
     * Missing database row for the principal id throws UserNotFoundException.
     */
    @Test
    void getCurrentUserThrowsWhenMissing() {
        UserDetailsImpl details = new UserDetailsImpl(7, "ghost", "hash", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        when(userRepository.findById(7)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, currentUserService::getCurrentUser);
    }
}
