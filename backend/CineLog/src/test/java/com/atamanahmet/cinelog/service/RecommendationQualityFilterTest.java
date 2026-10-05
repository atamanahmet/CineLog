package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;

/**
 * RecommendationQualityFilter keep and drop rules.
 */
class RecommendationQualityFilterTest {

    private RecommendationQualityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RecommendationQualityFilter(new RecommendationQualityProperties());
    }

    /**
     * Movie below 100 is dropped; movie at exactly 100 is kept.
     */
    @Test
    void movieFloorDropsBelowAndKeepsEqual() {
        RecommendationItemDTO below = item(1, TmdbMediaType.MOVIE, 99);
        RecommendationItemDTO equal = item(2, TmdbMediaType.MOVIE, 100);

        RecommendationQualityFilter.Result result = filter.apply(List.of(below, equal));

        assertThat(result.kept()).extracting(RecommendationItemDTO::id).containsExactly(2);
        assertThat(result.dropped()).isEqualTo(1);
    }

    /**
     * TV at 50 is kept; TV at 49 is dropped.
     */
    @Test
    void tvFloorDropsBelowAndKeepsEqual() {
        RecommendationItemDTO equal = item(1, TmdbMediaType.TV, 50);
        RecommendationItemDTO below = item(2, TmdbMediaType.TV, 49);

        RecommendationQualityFilter.Result result = filter.apply(List.of(equal, below));

        assertThat(result.kept()).extracting(RecommendationItemDTO::id).containsExactly(1);
        assertThat(result.dropped()).isEqualTo(1);
    }

    /**
     * Null voteCount is kept.
     */
    @Test
    void nullVoteCountIsKept() {
        RecommendationItemDTO missing = item(1, TmdbMediaType.MOVIE, null);

        RecommendationQualityFilter.Result result = filter.apply(List.of(missing));

        assertThat(result.kept()).hasSize(1);
        assertThat(result.dropped()).isZero();
    }

    /**
     * Keep order after mixed drops; movie and TV use their own floors.
     */
    @Test
    void preservesOrderAndUsesPerTypeFloors() {
        List<RecommendationItemDTO> input = List.of(
                item(1, TmdbMediaType.MOVIE, 100),
                item(2, TmdbMediaType.TV, 49),
                item(3, TmdbMediaType.MOVIE, 50),
                item(4, TmdbMediaType.TV, 50),
                item(5, TmdbMediaType.MOVIE, null));

        RecommendationQualityFilter.Result result = filter.apply(input);

        assertThat(result.kept()).extracting(RecommendationItemDTO::id).containsExactly(1, 4, 5);
        assertThat(result.dropped()).isEqualTo(2);
    }

    /**
     * Build one recommendation row for filter tests.
     */
    private static RecommendationItemDTO item(int id, TmdbMediaType mediaType, Integer voteCount) {
        return new RecommendationItemDTO(
                id,
                "Title-" + id,
                null,
                null,
                null,
                null,
                null,
                voteCount,
                0.5,
                mediaType,
                List.of());
    }
}
