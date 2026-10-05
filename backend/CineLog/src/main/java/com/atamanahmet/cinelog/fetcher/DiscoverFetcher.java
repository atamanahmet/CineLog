package com.atamanahmet.cinelog.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.mapper.MovieMapper;

@Component
public class DiscoverFetcher {

    public static final int TMDB_PAGE_SIZE = 20;

    private final TmdbClient tmdbClient;
    private final TmdbClient warmTmdbClient;
    private final MovieMapper movieMapper;

    public DiscoverFetcher(
            TmdbClient tmdbClient,
            @Qualifier("tmdbWarmClient") TmdbClient warmTmdbClient,
            MovieMapper movieMapper) {
        this.tmdbClient = tmdbClient;
        this.warmTmdbClient = warmTmdbClient;
        this.movieMapper = movieMapper;
    }

    /**
     * Call TMDB movie discover and map results to entities.
     */
    public List<Movie> fetch(DiscoverRequest request) {
        return fetchPage(tmdbClient, request);
    }

    /**
     * Fetch consecutive TMDB pages until maxItems is reached or results run out.
     */
    public List<Movie> fetchUpTo(DiscoverRequest request, int maxItems) {
        List<Movie> all = new ArrayList<>();
        int page = 1;
        while (all.size() < maxItems) {
            List<Movie> pageItems = fetchPage(warmTmdbClient, request.withPage(page));
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
     * Discover one movie page through the given TMDB client and map to entities.
     */
    private List<Movie> fetchPage(TmdbClient client, DiscoverRequest request) {
        return movieMapper.fromPage(client.discoverMovies(request));
    }
}
