package com.atamanahmet.cinelog.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.dto.DiscoverDefaultsResponse;
import com.atamanahmet.cinelog.dto.DiscoverRequest;

class DiscoverDefaultsMapperTest {

    /**
     * Defaults DTO values must equal DiscoverRequest constants.
     */
    @Test
    void fromConstantsMatchesDiscoverRequestConstants() {
        DiscoverDefaultsResponse response = DiscoverDefaultsMapper.fromConstants();
        assertEquals(DiscoverRequest.DEFAULT_MIN_VOTES, response.minVotes().defaultValue());
        assertEquals(DiscoverRequest.MAX_VOTE_COUNT, response.minVotes().max());
        assertEquals(DiscoverRequest.MIN_RUNTIME, response.runtime().min());
        assertEquals(DiscoverRequest.MAX_RUNTIME, response.runtime().max());
        assertEquals(DiscoverRequest.MIN_YEAR, response.year().min());
        assertEquals(DiscoverRequest.MAX_YEAR, response.year().max());
        assertEquals(DiscoverRequest.MIN_RATING, response.rating().min());
        assertEquals(DiscoverRequest.MAX_RATING, response.rating().max());
        assertEquals(DiscoverRequest.TMDB_MAX_PAGE, response.maxPage());
    }
}
