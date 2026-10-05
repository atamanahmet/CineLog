package com.atamanahmet.cinelog.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;

import lombok.RequiredArgsConstructor;

/**
 * Hydrate list MediaKey sets into MediaListItemDTO rows.
 */
@Component
@RequiredArgsConstructor
public class ListItemHydrator {

    private final MovieService movieService;
    private final TvShowService tvShowService;

    /**
     * Hydrate typed list keys into flat MediaListItemDTO rows (movie and TV).
     */
    public Set<MediaListItemDTO> hydrate(Set<MediaKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }
        Set<Integer> movieIds = keys.stream()
                .filter(key -> key.mediaType() == TmdbMediaType.MOVIE)
                .map(MediaKey::tmdbId)
                .collect(Collectors.toSet());
        Set<Integer> tvIds = keys.stream()
                .filter(key -> key.mediaType() == TmdbMediaType.TV)
                .map(MediaKey::tmdbId)
                .collect(Collectors.toSet());
        Set<MediaListItemDTO> items = new HashSet<>();
        if (!movieIds.isEmpty()) {
            for (Movie movie : movieService.getMoviesFromIdSet(movieIds)) {
                items.add(MediaListItemDTO.fromMovie(movie));
            }
        }
        if (!tvIds.isEmpty()) {
            for (TvShow tvShow : tvShowService.getTvShowsFromIdSet(tvIds)) {
                items.add(MediaListItemDTO.fromTvShow(tvShow));
            }
        }
        return items;
    }
}
