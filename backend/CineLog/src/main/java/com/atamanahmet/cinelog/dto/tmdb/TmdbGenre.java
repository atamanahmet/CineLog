package com.atamanahmet.cinelog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One TMDB genre object from a by-id details payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenre(int id, String name) {
}
