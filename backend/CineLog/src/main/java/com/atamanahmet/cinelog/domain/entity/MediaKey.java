package com.atamanahmet.cinelog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Typed (tmdbId, mediaType) pair so movie and TV list membership cannot collide on bare ids.
 */
@Embeddable
public record MediaKey(
        @Column(name = "tmdb_id", nullable = false) Integer tmdbId,
        @Enumerated(EnumType.STRING)
        @Column(name = "media_type", nullable = false, length = 16) TmdbMediaType mediaType) {

    public MediaKey {
        if (tmdbId == null) {
            throw new IllegalArgumentException("tmdbId must not be null");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
    }
}
