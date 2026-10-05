package com.atamanahmet.cinelog.dto.tmdb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TMDB genre list payload from /genre/movie/list and /genre/tv/list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenreListResponse(List<TmdbGenre> genres) {
}
