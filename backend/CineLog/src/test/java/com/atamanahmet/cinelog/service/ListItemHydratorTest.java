package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;

@ExtendWith(MockitoExtension.class)
class ListItemHydratorTest {

    @Mock
    private MovieService movieService;

    @Mock
    private TvShowService tvShowService;

    @InjectMocks
    private ListItemHydrator listItemHydrator;

    /**
     * Movie ids go only to MovieService; TV ids go only to TvShowService.
     */
    @Test
    void splitSendsIdsToMatchingServices() {
        Movie movie = movie(550, "Fight Club");
        TvShow tv = tv(1399, "Game of Thrones");
        when(movieService.getMoviesFromIdSet(Set.of(550))).thenReturn(List.of(movie));
        when(tvShowService.getTvShowsFromIdSet(Set.of(1399))).thenReturn(List.of(tv));

        Set<MediaListItemDTO> items = listItemHydrator.hydrate(Set.of(
                new MediaKey(550, TmdbMediaType.MOVIE),
                new MediaKey(1399, TmdbMediaType.TV)));

        verify(movieService).getMoviesFromIdSet(Set.of(550));
        verify(tvShowService).getTvShowsFromIdSet(Set.of(1399));
        assertThat(items).extracting(MediaListItemDTO::id).containsExactlyInAnyOrder(550, 1399);
        assertThat(items).extracting(MediaListItemDTO::mediaType)
                .containsExactlyInAnyOrder(TmdbMediaType.MOVIE, TmdbMediaType.TV);
    }

    /**
     * Empty and null keys return an empty set and skip both services.
     */
    @Test
    void emptySetReturnsEmptyWithoutServiceCalls() {
        assertThat(listItemHydrator.hydrate(Set.of())).isEmpty();
        assertThat(listItemHydrator.hydrate(null)).isEmpty();
        verify(movieService, never()).getMoviesFromIdSet(org.mockito.ArgumentMatchers.any());
        verify(tvShowService, never()).getTvShowsFromIdSet(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Mixed keys produce both movie and TV list items.
     */
    @Test
    void mixedKeysGiveBothKinds() {
        when(movieService.getMoviesFromIdSet(Set.of(1))).thenReturn(List.of(movie(1, "A")));
        when(tvShowService.getTvShowsFromIdSet(Set.of(2))).thenReturn(List.of(tv(2, "B")));

        Set<MediaListItemDTO> items = listItemHydrator.hydrate(Set.of(
                new MediaKey(1, TmdbMediaType.MOVIE),
                new MediaKey(2, TmdbMediaType.TV)));

        assertThat(items).hasSize(2);
        assertThat(items).anySatisfy(item -> {
            assertThat(item.id()).isEqualTo(1);
            assertThat(item.mediaType()).isEqualTo(TmdbMediaType.MOVIE);
        });
        assertThat(items).anySatisfy(item -> {
            assertThat(item.id()).isEqualTo(2);
            assertThat(item.mediaType()).isEqualTo(TmdbMediaType.TV);
        });
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
