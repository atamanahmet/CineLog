package com.atamanahmet.cinelog.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.atamanahmet.cinelog.cache.SortOption;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.validation.ValidDiscoverRequest;

import jakarta.validation.constraints.Min;

/**
 * Shared movie and TV discover query. languages is nullable because TV omits it by default.
 */
@ValidDiscoverRequest
public record DiscoverRequest(
        Boolean adult,
        String releaseWindow,
        Integer voteCount,
        SortOption sort,
        @Min(1) Integer page,
        List<Integer> genreIdList,
        int[] yearRange,
        int[] ratingRange,
        String languages,
        Integer minRuntime,
        Integer maxRuntime,
        List<Integer> withoutGenres) {

    public static final int DEFAULT_MIN_VOTES = 100;
    public static final int MAX_VOTE_COUNT = 10000;
    public static final int TMDB_MAX_PAGE = 500;
    public static final int MIN_RUNTIME = 0;
    public static final int MAX_RUNTIME = 400;
    public static final int MIN_YEAR = 1800;
    public static final int MAX_YEAR = 2100;
    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 10;
    public static final int DEFAULT_YEAR_MIN = 1900;
    public static final int DEFAULT_YEAR_MAX = 2040;

    /**
     * TMDB without_genres list separator. PIPE = exclude titles that match ANY
     * listed genre. Must be checked against TMDB behavior.
     */
    public static final String TMDB_WITHOUT_GENRES_SEPARATOR = "|";

    public DiscoverRequest {
        if (adult == null) {
            adult = Boolean.FALSE;
        }
        if (releaseWindow == null) {
            releaseWindow = "";
        }
        if (voteCount == null) {
            voteCount = DEFAULT_MIN_VOTES;
        }
        if (page == null) {
            page = 1;
        }
        if (genreIdList == null) {
            genreIdList = List.of();
        } else {
            genreIdList = List.copyOf(genreIdList);
        }
        if (withoutGenres == null) {
            withoutGenres = List.of();
        } else {
            withoutGenres = List.copyOf(withoutGenres);
        }
        if (yearRange == null || yearRange.length == 0) {
            yearRange = new int[] { DEFAULT_YEAR_MIN, DEFAULT_YEAR_MAX };
        } else {
            yearRange = Arrays.copyOf(yearRange, yearRange.length);
        }
        if (ratingRange == null || ratingRange.length == 0) {
            ratingRange = new int[] { MIN_RATING, MAX_RATING };
        } else {
            ratingRange = Arrays.copyOf(ratingRange, ratingRange.length);
        }
    }

    /**
     * Fill media-specific sort and language defaults. Shared fields are already filled in the compact constructor.
     */
    public DiscoverRequest withDefaults(TmdbMediaType mediaType) {
        SortOption sortValue = sort;
        if (sortValue == null) {
            sortValue = mediaType == TmdbMediaType.MOVIE ? SortOption.POPULARITY_DESC : SortOption.VOTE_COUNT_DESC;
        }
        String languageValue = languages;
        if (mediaType == TmdbMediaType.MOVIE && (languageValue == null || languageValue.isBlank())) {
            languageValue = "en";
        }
        return new DiscoverRequest(adult, releaseWindow, voteCount, sortValue, page, genreIdList, yearRange,
                ratingRange, languageValue, minRuntime, maxRuntime, withoutGenres);
    }

    /**
     * Same filters as a warmed cache entry for this sort and media type.
     */
    public static DiscoverRequest forWarm(SortOption sortOption, TmdbMediaType mediaType) {
        return new DiscoverRequest(
                Boolean.FALSE,
                "",
                DEFAULT_MIN_VOTES,
                sortOption,
                1,
                List.of(),
                new int[] { DEFAULT_YEAR_MIN, DEFAULT_YEAR_MAX },
                new int[] { MIN_RATING, MAX_RATING },
                mediaType == TmdbMediaType.MOVIE ? "en" : null,
                null,
                null,
                List.of());
    }

    /**
     * Copy with adult forced off unless the site allows it.
     */
    public DiscoverRequest restrictAdult(boolean adultAllowed) {
        if (adultAllowed || !Boolean.TRUE.equals(adult)) {
            return this;
        }
        return new DiscoverRequest(Boolean.FALSE, releaseWindow, voteCount, sort, page, genreIdList, yearRange,
                ratingRange, languages, minRuntime, maxRuntime, withoutGenres);
    }

    /**
     * Copy with a different TMDB page number.
     */
    public DiscoverRequest withPage(int pageNumber) {
        return new DiscoverRequest(adult, releaseWindow, voteCount, sort, pageNumber, genreIdList, yearRange,
                ratingRange, languages, minRuntime, maxRuntime, withoutGenres);
    }

    /**
     * True when either runtime bound is set for a live TMDB query.
     */
    public boolean hasRuntimeFilter() {
        return minRuntime != null || maxRuntime != null;
    }

    /**
     * Comma-separated genre ids for the TMDB with_genres query.
     */
    public String genreIdsCsv() {
        if (genreIdList == null || genreIdList.isEmpty()) {
            return "";
        }
        return genreIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * Joined genre ids for TMDB without_genres, or empty when none.
     */
    public String withoutGenresParam() {
        if (withoutGenres == null || withoutGenres.isEmpty()) {
            return "";
        }
        return withoutGenres.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(TMDB_WITHOUT_GENRES_SEPARATOR));
    }
}
