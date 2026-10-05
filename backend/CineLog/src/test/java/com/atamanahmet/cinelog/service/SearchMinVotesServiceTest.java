package com.atamanahmet.cinelog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.ContentPolicyProperties;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.SearchPageResponse;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPagedResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;
import com.atamanahmet.cinelog.fetcher.DiscoverFetcher;
import com.atamanahmet.cinelog.fetcher.TvDiscoverFetcher;
import com.atamanahmet.cinelog.mapper.MovieMapper;
import com.atamanahmet.cinelog.mapper.TvShowMapper;
import com.atamanahmet.cinelog.repository.MovieRepository;
import com.atamanahmet.cinelog.repository.TvShowRepository;
import com.atamanahmet.cinelog.service.impl.MovieServiceImpl;
import com.atamanahmet.cinelog.service.impl.TvShowServiceImpl;

@ExtendWith(MockitoExtension.class)
class SearchMinVotesServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TvShowRepository tvShowRepository;

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private TvShowMapper tvShowMapper;

    @Mock
    private DiscoverCacheService discoverCacheService;

    @Mock
    private DetailsCacheService detailsCacheService;

    @Mock
    private DiscoverFetcher discoverFetcher;

    @Mock
    private TvDiscoverFetcher tvDiscoverFetcher;

    private MovieServiceImpl movieService;
    private TvShowServiceImpl tvShowService;

    @BeforeEach
    void setUp() {
        ContentPolicyProperties contentPolicy = new ContentPolicyProperties(false);
        movieService = new MovieServiceImpl(
                movieRepository,
                tmdbClient,
                movieMapper,
                discoverCacheService,
                detailsCacheService,
                discoverFetcher,
                contentPolicy);
        tvShowService = new TvShowServiceImpl(
                tvShowRepository,
                tmdbClient,
                tvShowMapper,
                discoverCacheService,
                detailsCacheService,
                tvDiscoverFetcher,
                contentPolicy);
    }

    /**
     * Movie minVotes 100 drops 99, keeps 100, and drops a missing vote_count.
     */
    @Test
    void movieMinVotesFiltersByTmdbVoteCount() {
        stubMovieSearch(moviePage(
                tmdbMovie(1, 99),
                tmdbMovie(2, 100),
                tmdbMovie(3, null)));

        SearchPageResponse<MovieDto> result = movieService.searchMovies("q", 1, 100);

        assertEquals(List.of(2), result.results().stream().map(MovieDto::id).toList());
        assertEquals(1, result.page());
        assertEquals(4, result.totalPages());
        assertEquals(80, result.totalResults());
    }

    /**
     * Movie search without minVotes keeps every TMDB result.
     */
    @Test
    void movieAbsentMinVotesKeepsAll() {
        stubMovieSearch(moviePage(
                tmdbMovie(1, 99),
                tmdbMovie(2, 100),
                tmdbMovie(3, null)));

        SearchPageResponse<MovieDto> result = movieService.searchMovies("q", 1, null);

        assertEquals(List.of(1, 2, 3), result.results().stream().map(MovieDto::id).toList());
    }

    /**
     * TV minVotes 100 drops 99, keeps 100, and drops a missing vote_count.
     */
    @Test
    void tvMinVotesFiltersByTmdbVoteCount() {
        stubTvSearch(tvPage(
                tmdbTv(1, 99),
                tmdbTv(2, 100),
                tmdbTv(3, null)));

        SearchPageResponse<TvShowDto> result = tvShowService.searchTvShows("q", 1, 100);

        assertEquals(List.of(2), result.results().stream().map(TvShowDto::id).toList());
        assertEquals(1, result.page());
        assertEquals(4, result.totalPages());
        assertEquals(80, result.totalResults());
    }

    /**
     * TV search without minVotes keeps every TMDB result.
     */
    @Test
    void tvAbsentMinVotesKeepsAll() {
        stubTvSearch(tvPage(
                tmdbTv(1, 99),
                tmdbTv(2, 100),
                tmdbTv(3, null)));

        SearchPageResponse<TvShowDto> result = tvShowService.searchTvShows("q", 1, null);

        assertEquals(List.of(1, 2, 3), result.results().stream().map(TvShowDto::id).toList());
    }

    private void stubMovieSearch(TmdbPagedResponse<TmdbMovieResponse> page) {
        when(tmdbClient.searchMovies("q", 1)).thenReturn(page);
        when(movieMapper.fromPage(any())).thenAnswer(invocation -> {
            TmdbPagedResponse<TmdbMovieResponse> arg = invocation.getArgument(0);
            return arg.getResults().stream().map(r -> {
                Movie movie = new Movie();
                movie.setId(r.getId());
                movie.setVoteCount(r.getVoteCount());
                return movie;
            }).toList();
        });
        when(movieMapper.toDtos(any())).thenAnswer(invocation -> {
            List<Movie> movies = invocation.getArgument(0);
            return movies.stream()
                    .map(m -> new MovieDto(
                            m.getId(), false, null, List.of(), "en", "t", "o", 1.0, null, null, "t", false, 7.0,
                            m.getVoteCount()))
                    .toList();
        });
    }

    private void stubTvSearch(TmdbPagedResponse<TmdbTvShowResponse> page) {
        when(tmdbClient.searchTvShows("q", 1)).thenReturn(page);
        when(tvShowMapper.fromPage(any())).thenAnswer(invocation -> {
            TmdbPagedResponse<TmdbTvShowResponse> arg = invocation.getArgument(0);
            return arg.getResults().stream().map(r -> {
                TvShow tvShow = new TvShow();
                tvShow.setId(r.getId());
                tvShow.setVoteCount(r.getVoteCount());
                return tvShow;
            }).toList();
        });
        when(tvShowMapper.toDtos(any())).thenAnswer(invocation -> {
            List<TvShow> shows = invocation.getArgument(0);
            return shows.stream()
                    .map(s -> new TvShowDto(
                            s.getId(), false, null, List.of(), List.of(), "en", "n", "o", 1.0, null, null, "n", 7.0,
                            s.getVoteCount(), null, null, null, null, null, null, List.of(), List.of(), null,
                            List.of()))
                    .toList();
        });
    }

    private static TmdbPagedResponse<TmdbMovieResponse> moviePage(TmdbMovieResponse... results) {
        return new TmdbPagedResponse<>(1, List.of(results), 4, 80);
    }

    private static TmdbPagedResponse<TmdbTvShowResponse> tvPage(TmdbTvShowResponse... results) {
        return new TmdbPagedResponse<>(1, List.of(results), 4, 80);
    }

    private static TmdbMovieResponse tmdbMovie(Integer id, Integer voteCount) {
        TmdbMovieResponse response = new TmdbMovieResponse();
        response.setId(id);
        response.setVoteCount(voteCount);
        return response;
    }

    private static TmdbTvShowResponse tmdbTv(Integer id, Integer voteCount) {
        TmdbTvShowResponse response = new TmdbTvShowResponse();
        response.setId(id);
        response.setVoteCount(voteCount);
        return response;
    }
}
