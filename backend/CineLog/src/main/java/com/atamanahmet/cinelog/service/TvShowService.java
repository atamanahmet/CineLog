package com.atamanahmet.cinelog.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.SearchPageResponse;
import com.atamanahmet.cinelog.dto.TvShowDto;

public interface TvShowService {

    List<TvShowDto> discoverTvShows(DiscoverRequest params);

    SearchPageResponse<TvShowDto> searchTvShows(String query, int page, Integer minVotes);

    /**
     * Live TMDB detail fetch, mapped to TvShowDto. No persistence.
     */
    TvShowDto getDetailsFromTmdb(Integer id);

    /**
     * Return the cached YouTube trailer URL for this TV show, if any.
     */
    Optional<String> getTrailer(Integer id);

    /**
     * Return the cached top cast for this TV show.
     */
    List<CastMember> getTopCast(Integer id);

    void saveTvShow(TvShow tvShow);

    TvShow findTvShowById(Integer id);

    /**
     * Return local TV show, or fetch from TMDB, save, and return it.
     */
    TvShow findOrFetchTvShow(Integer id);

    List<TvShow> getTvShowsFromIdSet(Set<Integer> idSet);
}
