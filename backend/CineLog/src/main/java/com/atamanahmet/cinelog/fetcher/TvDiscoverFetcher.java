package com.atamanahmet.cinelog.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.mapper.TvShowMapper;

@Component
public class TvDiscoverFetcher {

    public static final int TMDB_PAGE_SIZE = 20;

    private final TmdbClient tmdbClient;
    private final TmdbClient warmTmdbClient;
    private final TvShowMapper tvShowMapper;

    public TvDiscoverFetcher(
            TmdbClient tmdbClient,
            @Qualifier("tmdbWarmClient") TmdbClient warmTmdbClient,
            TvShowMapper tvShowMapper) {
        this.tmdbClient = tmdbClient;
        this.warmTmdbClient = warmTmdbClient;
        this.tvShowMapper = tvShowMapper;
    }

    /**
     * Call TMDB TV discover and map results to entities.
     */
    public List<TvShow> fetch(DiscoverRequest request) {
        return fetchPage(tmdbClient, request);
    }

    /**
     * Fetch consecutive TMDB pages until maxItems is reached or results run out.
     */
    public List<TvShow> fetchUpTo(DiscoverRequest request, int maxItems) {
        List<TvShow> all = new ArrayList<>();
        int page = 1;
        while (all.size() < maxItems) {
            List<TvShow> pageItems = fetchPage(warmTmdbClient, request.withPage(page));
            if (pageItems.isEmpty()) {
                break;
            }
            all.addAll(pageItems);
            if (pageItems.size() < TMDB_PAGE_SIZE) {
                break;
            }
            page++;
        }
        if (all.size() > maxItems) {
            return List.copyOf(all.subList(0, maxItems));
        }
        return List.copyOf(all);
    }

    /**
     * Discover one TV page through the given TMDB client and map to entities.
     */
    private List<TvShow> fetchPage(TmdbClient client, DiscoverRequest request) {
        return tvShowMapper.fromPage(client.discoverTvShows(request));
    }
}
