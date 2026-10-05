package com.atamanahmet.cinelog.mapper;

import com.atamanahmet.cinelog.dto.DiscoverDefaultsResponse;
import com.atamanahmet.cinelog.dto.DiscoverDefaultsResponse.MinMaxBounds;
import com.atamanahmet.cinelog.dto.DiscoverDefaultsResponse.MinVotesBounds;
import com.atamanahmet.cinelog.dto.DiscoverRequest;

/**
 * Builds discover defaults from DiscoverRequest constants.
 */
public final class DiscoverDefaultsMapper {

    private DiscoverDefaultsMapper() {
    }

    /**
     * Map DiscoverRequest constants into the public defaults DTO.
     */
    public static DiscoverDefaultsResponse fromConstants() {
        return new DiscoverDefaultsResponse(
                new MinVotesBounds(DiscoverRequest.DEFAULT_MIN_VOTES, DiscoverRequest.MAX_VOTE_COUNT),
                new MinMaxBounds(DiscoverRequest.MIN_RUNTIME, DiscoverRequest.MAX_RUNTIME),
                new MinMaxBounds(DiscoverRequest.MIN_YEAR, DiscoverRequest.MAX_YEAR),
                new MinMaxBounds(DiscoverRequest.MIN_RATING, DiscoverRequest.MAX_RATING),
                DiscoverRequest.TMDB_MAX_PAGE);
    }
}
