package com.atamanahmet.cinelog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.CatalogCacheId;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

@Repository
public interface CatalogCacheRepository extends JpaRepository<CatalogCache, CatalogCacheId> {

    /**
     * Load catalog_cache rows for one media type and a set of TMDB ids.
     */
    List<CatalogCache> findByMediaTypeAndTmdbIdIn(TmdbMediaType mediaType, Collection<Integer> tmdbIds);
}
