package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.http.ResponseEntity;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.dto.UserDTO;
import com.atamanahmet.cinelog.mapper.UserMapper;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserArchiveOmitsRejectedTest {

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
        org.mockito.Mockito.lenient()
                .when(recommendationExclusionService.allExcluded(any()))
                .thenReturn(Set.of());
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
     * Archive never loads REJECTED entries into UserDTO.
     */
    @Test
    void archiveOutputContainsNoRejectedEntries() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                eq(7), any(TmdbMediaType.class), any(ListType.class)))
                .thenReturn(List.of());
        when(listItemHydrator.hydrate(any())).thenReturn(Set.of());
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

        ResponseEntity<?> response = userService.getUserArchive(null);

        UserDTO body = (UserDTO) response.getBody();
        assertThat(body.watchlist()).isEmpty();
        assertThat(body.watchedlist()).isEmpty();
        assertThat(body.lovedlist()).isEmpty();
        verify(userListEntryRepository, never()).findByUserIdAndMediaTypeAndListType(
                eq(7), any(), eq(ListType.REJECTED));
        verify(userListEntryRepository, never()).findByUserIdAndListType(eq(7), eq(ListType.REJECTED));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<MediaKey>> lovedKeys = ArgumentCaptor.forClass(Set.class);
        verify(userMapper).toDto(any(), any(), any(), lovedKeys.capture(), any(), any(), any(), any());
        assertThat(lovedKeys.getValue()).isEmpty();
    }
}
