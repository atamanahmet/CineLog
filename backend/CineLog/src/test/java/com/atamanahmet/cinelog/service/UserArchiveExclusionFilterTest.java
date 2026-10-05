package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.UserDTO;
import com.atamanahmet.cinelog.mapper.UserMapper;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserArchiveExclusionFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserListEntryRepository userListEntryRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RecommendationHydrator recommendationHydrator;

    @Mock
    private ListItemHydrator listItemHydrator;

    @Mock
    private RecommendationExclusionService recommendationExclusionService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userListEntryRepository,
                userMapper,
                currentUserService,
                recommendationHydrator,
                listItemHydrator,
                recommendationExclusionService);
    }

    /**
     * Stored recommendation keys in the exclusion set are not passed to hydrateCacheOnly.
     */
    @Test
    void archiveSkipsExcludedStoredKeysBeforeHydration() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.replaceRecommendation(TmdbMediaType.MOVIE, List.of(
                new MediaKey(101, TmdbMediaType.MOVIE),
                new MediaKey(202, TmdbMediaType.MOVIE),
                new MediaKey(303, TmdbMediaType.MOVIE)));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                eq(7), any(TmdbMediaType.class), any(ListType.class)))
                .thenReturn(List.of());
        when(listItemHydrator.hydrate(any())).thenReturn(Set.of());
        when(recommendationExclusionService.allExcluded(7)).thenReturn(Set.of(
                new MediaKey(101, TmdbMediaType.MOVIE),
                new MediaKey(303, TmdbMediaType.MOVIE)));
        when(recommendationHydrator.hydrateCacheOnly(any())).thenReturn(List.of());
        when(userMapper.toDto(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new UserDTO(
                        null,
                        inv.getArgument(1),
                        inv.getArgument(2),
                        inv.getArgument(3),
                        inv.getArgument(4),
                        inv.getArgument(5),
                        inv.getArgument(6),
                        inv.getArgument(7)));

        userService.getUserArchive(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MediaKey>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(recommendationHydrator).hydrateCacheOnly(keysCaptor.capture());
        assertThat(keysCaptor.getValue()).containsExactly(new MediaKey(202, TmdbMediaType.MOVIE));
    }
}
