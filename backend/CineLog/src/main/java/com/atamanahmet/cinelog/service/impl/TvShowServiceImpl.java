package com.atamanahmet.cinelog.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.ContentPolicyProperties;
import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.dto.SearchPageResponse;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPagedResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;
import com.atamanahmet.cinelog.mapper.TvShowMapper;
import com.atamanahmet.cinelog.repository.TvShowRepository;
import com.atamanahmet.cinelog.fetcher.TvDiscoverFetcher;
import com.atamanahmet.cinelog.service.DetailsBundle;
import com.atamanahmet.cinelog.service.DetailsCacheService;
import com.atamanahmet.cinelog.service.DiscoverCacheService;
import com.atamanahmet.cinelog.service.TvShowService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TvShowServiceImpl implements TvShowService {

    private final TvShowRepository tvShowRepository;
    private final TmdbClient tmdbClient;
    private final TvShowMapper tvShowMapper;
    private final DiscoverCacheService discoverCacheService;
    private final DetailsCacheService detailsCacheService;
    private final TvDiscoverFetcher tvDiscoverFetcher;
    private final ContentPolicyProperties contentPolicy;

    /**
     * Fetch TMDB TV discover results as DTOs, from cache when possible.
     */
    @Override
    public List<TvShowDto> discoverTvShows(DiscoverRequest params) {
        DiscoverRequest filled = params.withDefaults(TmdbMediaType.TV)
                .restrictAdult(contentPolicy.adultEnabled());
        if (filled.page() > DiscoverRequest.TMDB_MAX_PAGE) {
            return List.of();
        }
        List<TvShowDto> cached = discoverCacheService.getCachedTvShows(filled);
        if (cached != null) {
            return cached;
        }
        return tvShowMapper.toDtos(tvDiscoverFetcher.fetch(filled));
    }

    /**
     * Fetch one TMDB TV search page as DTOs, keeping pagination metadata.
     */
    @Override
    public SearchPageResponse<TvShowDto> searchTvShows(String query, int page, Integer minVotes) {
        TmdbPagedResponse<TmdbTvShowResponse> tmdbPage = tmdbClient.searchTvShows(query, page);
        if (minVotes != null && tmdbPage != null && tmdbPage.getResults() != null) {
            int min = minVotes;
            tmdbPage.setResults(tmdbPage.getResults().stream()
                    .filter(item -> (item.getVoteCount() == null ? 0 : item.getVoteCount()) >= min)
                    .toList());
        }
        List<TvShowDto> results = tvShowMapper.toDtos(tvShowMapper.fromPage(tmdbPage));
        return new SearchPageResponse<>(tmdbPage.getPage(), results, tmdbPage.getTotalPages(),
                tmdbPage.getTotalResults());
    }

    /**
     * Fetch one TV show from TMDB and map it. Does not write to the database.
     */
    @Override
    public TvShowDto getDetailsFromTmdb(Integer id) {
        return detailsCacheService.getTvShowDetails(id);
    }

    /**
     * Return the cached YouTube trailer URL for this TV show, if any.
     */
    @Override
    public Optional<String> getTrailer(Integer id) {
        DetailsBundle<TvShowDto> bundle = detailsCacheService.getTvShowBundle(id);
        if (bundle == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bundle.trailerUrl());
    }

    /**
     * Return the cached top cast for this TV show.
     */
    @Override
    public List<CastMember> getTopCast(Integer id) {
        DetailsBundle<TvShowDto> bundle = detailsCacheService.getTvShowBundle(id);
        if (bundle == null) {
            return List.of();
        }
        return bundle.topCast();
    }

    @Override
    @Transactional
    public void saveTvShow(TvShow tvShow) {
        tvShowRepository.save(tvShow);
    }

    @Override
    @Transactional(readOnly = true)
    public TvShow findTvShowById(Integer id) {
        return tvShowRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsTvShow(Integer id) {
        return tvShowRepository.existsById(id);
    }

    /**
     * Return local TV show, or fetch from TMDB, save, and return it.
     */
    @Override
    @Transactional
    public TvShow findOrFetchTvShow(Integer id) {
        TvShow existing = findTvShowById(id);
        if (existing != null) {
            return existing;
        }
        TmdbTvShowResponse response = tmdbClient.getTvShowById(id);
        if (response == null) {
            return null;
        }
        TvShow tvShow = tvShowMapper.toEntity(response);
        try {
            saveTvShow(tvShow);
            return tvShow;
        } catch (DataIntegrityViolationException e) {
            return findTvShowById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TvShow> getTvShowsFromIdSet(Set<Integer> idSet) {
        return tvShowRepository.findAllById(idSet);
    }
}
