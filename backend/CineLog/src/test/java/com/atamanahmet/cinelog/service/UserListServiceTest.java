package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;

@ExtendWith(MockitoExtension.class)
class UserListServiceTest {

    @Mock
    private UserListEntryRepository userListEntryRepository;

    @Mock
    private MovieService movieService;

    @Mock
    private TvShowService tvShowService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ListItemHydrator listItemHydrator;

    @InjectMocks
    private UserListService userListService;

    private User user;
    private Movie movie;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7);
        user.setUsername("alice");
        movie = new Movie();
        movie.setId(42);
    }

    /**
     * Remove deletes the matching list entry.
     */
    @Test
    void removeDeletesEntry() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findMovieById(42)).thenReturn(movie);

        ResponseEntity<?> response = userListService.removeFromList(
                "42", TmdbMediaType.MOVIE, ListType.WATCHLIST, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userListEntryRepository).deleteByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.WATCHLIST);
    }

    /**
     * Remove still returns no content when the entry is already gone.
     */
    @Test
    void removeAbsentEntryIsNoOp() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findMovieById(42)).thenReturn(movie);

        ResponseEntity<?> response = userListService.removeFromList(
                "42", TmdbMediaType.MOVIE, ListType.LOVED, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userListEntryRepository).deleteByUserIdAndTmdbIdAndMediaTypeAndListType(
                eq(7), eq(42), eq(TmdbMediaType.MOVIE), eq(ListType.LOVED));
    }

    /**
     * getListItems hydrates only keys of the requested list type.
     */
    @Test
    void getListItemsReturnsOnlyRequestedTypeViaHydrator() {
        UserListEntry rejected = new UserListEntry();
        rejected.setUserId(7);
        rejected.setTmdbId(42);
        rejected.setMediaType(TmdbMediaType.MOVIE);
        rejected.setListType(ListType.REJECTED);
        when(userListEntryRepository.findByUserIdAndListType(7, ListType.REJECTED))
                .thenReturn(List.of(rejected));
        Set<MediaListItemDTO> hydrated = Set.of(
                new MediaListItemDTO(42, "Title", "Title", null, null, null, TmdbMediaType.MOVIE));
        when(listItemHydrator.hydrate(Set.of(new MediaKey(42, TmdbMediaType.MOVIE))))
                .thenReturn(hydrated);

        Set<MediaListItemDTO> result = userListService.getListItems(7, ListType.REJECTED);

        assertThat(result).isEqualTo(hydrated);
        verify(listItemHydrator).hydrate(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        verify(userListEntryRepository).findByUserIdAndListType(7, ListType.REJECTED);
        verify(userListEntryRepository, never()).findByUserIdAndListType(eq(7), eq(ListType.WATCHLIST));
    }

    /**
     * Empty list type returns an empty set and still calls the hydrator.
     */
    @Test
    void getListItemsEmptyReturnsEmptySet() {
        when(userListEntryRepository.findByUserIdAndListType(7, ListType.REJECTED))
                .thenReturn(List.of());
        when(listItemHydrator.hydrate(Set.of())).thenReturn(Set.of());

        Set<MediaListItemDTO> result = userListService.getListItems(7, ListType.REJECTED);

        assertThat(result).isEmpty();
        verify(listItemHydrator).hydrate(Set.of());
    }
}
