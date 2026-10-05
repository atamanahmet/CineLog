package com.atamanahmet.cinelog.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.cache.SortOption;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;

class DiscoverRequestDefaultsTest {

    /**
     * withDefaults fills voteCount from DEFAULT_MIN_VOTES.
     */
    @Test
    void withDefaultsUsesDefaultMinVotes100() {
        DiscoverRequest filled = new DiscoverRequest(null, null, null, null, null, null, null, null, null, null, null, null)
                .withDefaults(TmdbMediaType.MOVIE);
        assertEquals(100, DiscoverRequest.DEFAULT_MIN_VOTES);
        assertEquals(DiscoverRequest.DEFAULT_MIN_VOTES, filled.voteCount());
    }

    /**
     * forWarm uses DEFAULT_MIN_VOTES and leaves runtime unset.
     */
    @Test
    void forWarmUsesDefaultMinVotes100AndNoRuntime() {
        DiscoverRequest warm = DiscoverRequest.forWarm(SortOption.RATING_DESC, TmdbMediaType.TV);
        assertEquals(DiscoverRequest.DEFAULT_MIN_VOTES, warm.voteCount());
        assertEquals(100, warm.voteCount());
        assertNull(warm.minRuntime());
        assertNull(warm.maxRuntime());
    }
}
