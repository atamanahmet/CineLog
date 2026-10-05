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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.dto.UserDTO;
import com.atamanahmet.cinelog.mapper.UserMapper;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserArchiveHydrationCharacterizationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserListEntryRepository userListEntryRepository;

    @Mock
    private MovieService movieService;

    @Mock
    private TvShowService tvShowService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RecommendationHydrator recommendationHydrator;

    @Mock
    private RecommendationExclusionService recommendationExclusionService;

    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setProfilePictureUrl(null);
        ListItemHydrator listItemHydrator = new ListItemHydrator(movieService, tvShowService);
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
     * Archive watchlist hydrates one movie and one TV into MediaListItemDTO rows.
     */
    @Test
    void archiveHydratesMovieAndTvListItems() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                7, TmdbMediaType.MOVIE, ListType.WATCHLIST))
                .thenReturn(List.of(entry(550, TmdbMediaType.MOVIE, ListType.WATCHLIST)));
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                7, TmdbMediaType.TV, ListType.WATCHLIST))
                .thenReturn(List.of(entry(1399, TmdbMediaType.TV, ListType.WATCHLIST)));
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                eq(7), any(TmdbMediaType.class), eq(ListType.WATCHED)))
                .thenReturn(List.of());
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                eq(7), any(TmdbMediaType.class), eq(ListType.LOVED)))
                .thenReturn(List.of());

        Movie movie = movie(550, "Fight Club");
        TvShow tv = tv(1399, "Game of Thrones");
        when(movieService.getMoviesFromIdSet(Set.of(550))).thenReturn(List.of(movie));
        when(tvShowService.getTvShowsFromIdSet(Set.of(1399))).thenReturn(List.of(tv));
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserDTO body = (UserDTO) response.getBody();
        assertThat(body.watchlist()).extracting(MediaListItemDTO::id)
                .containsExactlyInAnyOrder(550, 1399);
        assertThat(body.watchlist()).extracting(MediaListItemDTO::mediaType)
                .containsExactlyInAnyOrder(TmdbMediaType.MOVIE, TmdbMediaType.TV);
        assertThat(body.watchlist()).allSatisfy(item -> {
            assertThat(item).isInstanceOf(MediaListItemDTO.class);
            assertThat(item.title()).isNotBlank();
        });
    }

    /**
     * Empty list keys produce an empty hydrated set.
     */
    @Test
    void archiveEmptyListGivesEmptyHydratedResult() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userListEntryRepository.findByUserIdAndMediaTypeAndListType(
                eq(7), any(TmdbMediaType.class), any(ListType.class)))
                .thenReturn(List.of());
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
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<MediaListItemDTO>> watchlistItems =
                ArgumentCaptor.forClass(Set.class);
        verify(userMapper).toDto(
                any(), any(), any(), any(),
                watchlistItems.capture(), any(), any(), any());
        assertThat(watchlistItems.getValue()).isEmpty();
    }

    private static UserListEntry entry(int tmdbId, TmdbMediaType mediaType, ListType listType) {
        UserListEntry e = new UserListEntry();
        e.setUserId(7);
        e.setTmdbId(tmdbId);
        e.setMediaType(mediaType);
        e.setListType(listType);
        return e;
    }

    private static Movie movie(int id, String title) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle(title);
        m.setOriginalTitle(title);
        return m;
    }

    private static TvShow tv(int id, String title) {
        TvShow t = new TvShow();
        t.setId(id);
        t.setTitle(title);
        t.setOriginalTitle(title);
        return t;
    }
}
