package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import com.atamanahmet.cinelog.exception.UserNotFoundException;
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
     * First add saves one list entry for the resolved media id.
     */
    @Test
    void addCreatesAnEntry() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.WATCHLIST)).thenReturn(false);

        ResponseEntity<?> response = userListService.addToList(
                "42", TmdbMediaType.MOVIE, ListType.WATCHLIST, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ArgumentCaptor<UserListEntry> captor = ArgumentCaptor.forClass(UserListEntry.class);
        verify(userListEntryRepository).save(captor.capture());
        UserListEntry saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7);
        assertThat(saved.getTmdbId()).isEqualTo(42);
        assertThat(saved.getMediaType()).isEqualTo(TmdbMediaType.MOVIE);
        assertThat(saved.getListType()).isEqualTo(ListType.WATCHLIST);
    }

    /**
     * Second add skips save when the entry already exists.
     */
    @Test
    void addTwiceLeavesOneEntry() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.WATCHLIST))
                .thenReturn(false, true);

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.WATCHLIST, null);
        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.WATCHLIST, null);

        verify(userListEntryRepository, times(1)).save(any(UserListEntry.class));
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
     * Repository calls receive the path mediaType, listType and resolved id.
     */
    @Test
    void mediaTypeListTypeAndIdReachRepository() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(99)).thenReturn(movieWithId(99));
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 99, TmdbMediaType.MOVIE, ListType.LOVED)).thenReturn(false);

        userListService.addToList("99", TmdbMediaType.MOVIE, ListType.LOVED, null);

        verify(userListEntryRepository).existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 99, TmdbMediaType.MOVIE, ListType.LOVED);
        ArgumentCaptor<UserListEntry> captor = ArgumentCaptor.forClass(UserListEntry.class);
        verify(userListEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(99);
        assertThat(captor.getValue().getMediaType()).isEqualTo(TmdbMediaType.MOVIE);
        assertThat(captor.getValue().getListType()).isEqualTo(ListType.LOVED);
    }

    /**
     * Unknown current user propagates UserNotFoundException.
     */
    @Test
    void unknownUserPropagatesUserNotFound() {
        when(currentUserService.getCurrentUser()).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> userListService.addToList(
                "42", TmdbMediaType.MOVIE, ListType.WATCHLIST, null))
                .isInstanceOf(UserNotFoundException.class);
        verify(userListEntryRepository, never()).save(any());
    }

    /**
     * Adding REJECTED deletes the same title from WATCHLIST and LOVED.
     */
    @Test
    void addRejectedRemovesWatchlistAndLoved() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.REJECTED)).thenReturn(false);

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.REJECTED, null);

        verify(userListEntryRepository).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                eq(7), eq(42), eq(TmdbMediaType.MOVIE),
                eq(Set.of(ListType.WATCHLIST, ListType.LOVED)));
        verify(userListEntryRepository).save(any(UserListEntry.class));
    }

    /**
     * Adding WATCHLIST or LOVED deletes REJECTED for the same title.
     */
    @Test
    void addWatchlistOrLovedRemovesRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.WATCHLIST)).thenReturn(false);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.LOVED)).thenReturn(false);

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.WATCHLIST, null);
        verify(userListEntryRepository).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                7, 42, TmdbMediaType.MOVIE, Set.of(ListType.REJECTED));

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.LOVED, null);
        verify(userListEntryRepository, times(2)).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                7, 42, TmdbMediaType.MOVIE, Set.of(ListType.REJECTED));
    }

    /**
     * WATCHED and REJECTED do not remove each other.
     */
    @Test
    void watchedAndRejectedNeverTouchEachOther() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                any(), any(), any(), any())).thenReturn(false);

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.WATCHED, null);
        verify(userListEntryRepository, never()).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                any(), any(), any(), any());

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.REJECTED, null);
        ArgumentCaptor<Set<ListType>> conflicts = ArgumentCaptor.forClass(Set.class);
        verify(userListEntryRepository).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                eq(7), eq(42), eq(TmdbMediaType.MOVIE), conflicts.capture());
        assertThat(conflicts.getValue()).doesNotContain(ListType.WATCHED);
    }

    /**
     * Conflict delete uses the same user, tmdb id and media type only.
     */
    @Test
    void differentTmdbIdOrMediaTypeUntouched() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.REJECTED)).thenReturn(false);

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.REJECTED, null);

        verify(userListEntryRepository).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                7, 42, TmdbMediaType.MOVIE, Set.of(ListType.WATCHLIST, ListType.LOVED));
        verify(userListEntryRepository, never()).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                eq(7), eq(99), any(), any());
        verify(userListEntryRepository, never()).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                eq(7), eq(42), eq(TmdbMediaType.TV), any());
    }

    /**
     * Double add of REJECTED saves once and still clears conflicts each time.
     */
    @Test
    void doubleAddRejectedLeavesOneEntry() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(movieService.findOrFetchMovie(42)).thenReturn(movie);
        when(userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                7, 42, TmdbMediaType.MOVIE, ListType.REJECTED))
                .thenReturn(false, true);

        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.REJECTED, null);
        userListService.addToList("42", TmdbMediaType.MOVIE, ListType.REJECTED, null);

        verify(userListEntryRepository, times(2)).deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                7, 42, TmdbMediaType.MOVIE, Set.of(ListType.WATCHLIST, ListType.LOVED));
        verify(userListEntryRepository, times(1)).save(any(UserListEntry.class));
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

    private static Movie movieWithId(int id) {
        Movie m = new Movie();
        m.setId(id);
        return m;
    }
}
