package com.atamanahmet.cinelog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.cache.SortOption;
import com.atamanahmet.cinelog.config.DiscoverCacheProperties;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.github.benmanes.caffeine.cache.Caffeine;

class DiscoverCacheKeyTest {

    /**
     * A default movie request uses the same SortOption key as the warmed movie cache.
     */
    @Test
    void defaultMovieRequestMapsToWarmedKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, null);
        assertEquals(SortOption.POPULARITY_DESC, service.warmedCacheKey(request, TmdbMediaType.MOVIE));
        assertEquals(DiscoverRequest.forWarm(SortOption.POPULARITY_DESC, TmdbMediaType.MOVIE).sort(),
                request.withDefaults(TmdbMediaType.MOVIE).sort());
    }

    /**
     * A default TV request uses the same SortOption key as the warmed TV cache.
     */
    @Test
    void defaultTvRequestMapsToWarmedKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, null);
        assertEquals(SortOption.VOTE_COUNT_DESC, service.warmedCacheKey(request, TmdbMediaType.TV));
        assertEquals(DiscoverRequest.forWarm(SortOption.VOTE_COUNT_DESC, TmdbMediaType.TV).sort(),
                request.withDefaults(TmdbMediaType.TV).sort());
    }

    /**
     * Non-default movie voteCount must not map to a warmed cache key.
     */
    @Test
    void voteCountZeroMovieDoesNotMapToCacheKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(null, null, 0, null, null, null, null, null, null, null, null, null, null);
        assertNull(service.warmedCacheKey(request, TmdbMediaType.MOVIE));
    }

    /**
     * Non-default TV voteCount must not map to a warmed cache key.
     */
    @Test
    void voteCountZeroTvDoesNotMapToCacheKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(null, null, 0, null, null, null, null, null, null, null, null, null, null);
        assertNull(service.warmedCacheKey(request, TmdbMediaType.TV));
    }

    /**
     * Movie runtime filter must not map to a warmed cache key.
     */
    @Test
    void runtimeSetMovieDoesNotMapToCacheKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(null, null, null, null, null, null, null, null, null, 60, 120, null, null);
        assertNull(service.warmedCacheKey(request, TmdbMediaType.MOVIE));
    }

    /**
     * TV runtime filter must not map to a warmed cache key.
     */
    @Test
    void runtimeSetTvDoesNotMapToCacheKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(null, null, null, null, null, null, null, null, null, 60, 120, null, null);
        assertNull(service.warmedCacheKey(request, TmdbMediaType.TV));
    }

    /**
     * withoutGenres is not the default view; live path only.
     */
    @Test
    void withoutGenresDoesNotMapToCacheKey() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(
                null, null, null, null, null, null, null, null, null, null, null, java.util.List.of(16), null);
        assertNull(service.warmedCacheKey(request, TmdbMediaType.MOVIE));
        assertNull(service.warmedCacheKey(request, TmdbMediaType.TV));
    }

    /**
     * Upcoming never uses the undated SortOption warmed cache.
     */
    @Test
    void upcomingDoesNotMapToCacheKeyEvenWithDefaultVotes() {
        DiscoverCacheService service = newService();
        DiscoverRequest request = new DiscoverRequest(
                null, null, DiscoverRequest.DEFAULT_MIN_VOTES, null, null, null, null, null, null, null, null, null,
                true);
        assertNull(service.warmedCacheKey(request, TmdbMediaType.MOVIE));
        assertNull(service.warmedCacheKey(request, TmdbMediaType.TV));
    }

    private static DiscoverCacheService newService() {
        return new DiscoverCacheService(
                Caffeine.newBuilder().build(),
                Caffeine.newBuilder().build(),
                null,
                null,
                null,
                null,
                new DiscoverCacheProperties());
    }
}
