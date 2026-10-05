package com.atamanahmet.cinelog.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;

/**
 * Drops recommendation rows whose vote count is below the media-type floor.
 */
@Component
public class RecommendationQualityFilter {

    private final RecommendationQualityProperties properties;

    public RecommendationQualityFilter(RecommendationQualityProperties properties) {
        this.properties = properties;
    }

    /**
     * Keep items at or above the floor; null voteCount stays. Order preserved.
     */
    public Result apply(List<RecommendationItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return new Result(List.of(), 0);
        }
        List<RecommendationItemDTO> kept = new ArrayList<>(items.size());
        int dropped = 0;
        for (RecommendationItemDTO item : items) {
            if (keep(item)) {
                kept.add(item);
            } else {
                dropped++;
            }
        }
        return new Result(List.copyOf(kept), dropped);
    }

    /**
     * True when voteCount is null or at least the floor for that media type.
     */
    private boolean keep(RecommendationItemDTO item) {
        Integer voteCount = item.voteCount();
        if (voteCount == null) {
            return true;
        }
        int floor = item.mediaType() == TmdbMediaType.TV
                ? properties.getMinVotesTv()
                : properties.getMinVotesMovie();
        return voteCount >= floor;
    }

    /**
     * Kept rows plus how many were dropped.
     */
    public record Result(List<RecommendationItemDTO> kept, int dropped) {
    }
}
