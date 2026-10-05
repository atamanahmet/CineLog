package com.atamanahmet.cinelog.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CatalogCacheRepositoryIdPairsTest {

    @Autowired
    private CatalogCacheRepository catalogCacheRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Derived per-type id lookup returns both types and skips missing ids.
     */
    @Test
    void findByMediaTypeAndTmdbIdInReturnsHitsAndSkipsMissing() {
        CatalogCache movie = new CatalogCache();
        movie.setTmdbId(101);
        movie.setMediaType(TmdbMediaType.MOVIE);
        movie.setTitle("Heat");
        catalogCacheRepository.save(movie);

        CatalogCache tv = new CatalogCache();
        tv.setTmdbId(202);
        tv.setMediaType(TmdbMediaType.TV);
        tv.setTitle("Show");
        catalogCacheRepository.save(tv);
        entityManager.flush();

        List<CatalogCache> movies = catalogCacheRepository.findByMediaTypeAndTmdbIdIn(
                TmdbMediaType.MOVIE, List.of(101, 999));
        List<CatalogCache> shows = catalogCacheRepository.findByMediaTypeAndTmdbIdIn(
                TmdbMediaType.TV, List.of(202));

        assertEquals(1, movies.size());
        assertEquals(101, movies.get(0).getTmdbId());
        assertEquals(TmdbMediaType.MOVIE, movies.get(0).getMediaType());

        assertEquals(1, shows.size());
        assertEquals(202, shows.get(0).getTmdbId());
        assertEquals(TmdbMediaType.TV, shows.get(0).getMediaType());

        Set<Integer> movieIds = movies.stream().map(CatalogCache::getTmdbId).collect(Collectors.toSet());
        assertTrue(movieIds.contains(101));
        assertTrue(!movieIds.contains(999));
    }
}
