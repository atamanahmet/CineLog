package com.atamanahmet.cinelog.repository;

import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

/**
 * Projection of a list entry key without loading the entity.
 */
public interface MediaKeyView {

    Integer getTmdbId();

    TmdbMediaType getMediaType();
}
