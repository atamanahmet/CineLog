package com.atamanahmet.cinelog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.ContentPolicyProperties;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.fetcher.DiscoverFetcher;
import com.atamanahmet.cinelog.fetcher.TvDiscoverFetcher;
import com.atamanahmet.cinelog.mapper.MovieMapper;
import com.atamanahmet.cinelog.mapper.TvShowMapper;
import com.atamanahmet.cinelog.repository.MovieRepository;
import com.atamanahmet.cinelog.repository.TvShowRepository;
import com.atamanahmet.cinelog.service.impl.MovieServiceImpl;
import com.atamanahmet.cinelog.service.impl.TvShowServiceImpl;

@ExtendWith(MockitoExtension.class)
class DiscoverServiceTest {

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
     * voteCount 0 skips the movie cache and sends vote_count.gte 0 to the fetcher.
     */
    @Test
    void voteCountZeroMovieBypassesCacheAndCallsFetcher() {
        when(discoverCacheService.getCachedMovies(any())).thenReturn(null);
        when(discoverFetcher.fetch(any())).thenReturn(List.of());
        when(movieMapper.toDtos(List.of())).thenReturn(List.of());

        movieService.discoverMovies(requestWithVoteCount(0));

        verify(discoverCacheService).getCachedMovies(any());
        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(discoverFetcher).fetch(captor.capture());
        assertEquals(0, captor.getValue().voteCount());
    }

    /**
     * voteCount 0 skips the TV cache and sends vote_count.gte 0 to the fetcher.
     */
    @Test
    void voteCountZeroTvBypassesCacheAndCallsFetcher() {
        when(discoverCacheService.getCachedTvShows(any())).thenReturn(null);
        when(tvDiscoverFetcher.fetch(any())).thenReturn(List.of());
        when(tvShowMapper.toDtos(List.of())).thenReturn(List.of());

        tvShowService.discoverTvShows(requestWithVoteCount(0));

        verify(discoverCacheService).getCachedTvShows(any());
        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(tvDiscoverFetcher).fetch(captor.capture());
        assertEquals(0, captor.getValue().voteCount());
    }

    /**
     * Default voteCount hits the movie cache and makes zero TMDB calls.
     */
    @Test
    void defaultVoteCountMovieUsesCacheWithoutTmdb() {
        MovieDto dto = sampleMovieDto();
        when(discoverCacheService.getCachedMovies(any())).thenReturn(List.of(dto));

        List<MovieDto> result = movieService.discoverMovies(requestWithVoteCount(null));

        assertEquals(List.of(dto), result);
        verify(discoverFetcher, never()).fetch(any());
        verify(tmdbClient, never()).discoverMovies(any());
    }

    /**
     * Default voteCount hits the TV cache and makes zero TMDB calls.
     */
    @Test
    void defaultVoteCountTvUsesCacheWithoutTmdb() {
        TvShowDto dto = sampleTvDto();
        when(discoverCacheService.getCachedTvShows(any())).thenReturn(List.of(dto));

        List<TvShowDto> result = tvShowService.discoverTvShows(requestWithVoteCount(null));

        assertEquals(List.of(dto), result);
        verify(tvDiscoverFetcher, never()).fetch(any());
        verify(tmdbClient, never()).discoverTvShows(any());
    }

    /**
     * Page above the TMDB cap returns empty movies with zero TMDB calls.
     */
    @Test
    void pageAboveMaxMovieReturnsEmptyWithoutTmdb() {
        List<MovieDto> result = movieService.discoverMovies(requestWithPage(501));

        assertTrue(result.isEmpty());
        verify(discoverFetcher, never()).fetch(any());
        verify(tmdbClient, never()).discoverMovies(any());
        verify(discoverCacheService, never()).getCachedMovies(any());
    }

    /**
     * Page above the TMDB cap returns empty TV with zero TMDB calls.
     */
    @Test
    void pageAboveMaxTvReturnsEmptyWithoutTmdb() {
        List<TvShowDto> result = tvShowService.discoverTvShows(requestWithPage(501));

        assertTrue(result.isEmpty());
        verify(tvDiscoverFetcher, never()).fetch(any());
        verify(tmdbClient, never()).discoverTvShows(any());
        verify(discoverCacheService, never()).getCachedTvShows(any());
    }

    /**
     * Page at the TMDB cap still calls TMDB for movies.
     */
    @Test
    void pageAtMaxMovieCallsTmdb() {
        when(discoverCacheService.getCachedMovies(any())).thenReturn(null);
        when(discoverFetcher.fetch(any())).thenReturn(List.<Movie>of());
        when(movieMapper.toDtos(List.of())).thenReturn(List.of());

        movieService.discoverMovies(requestWithPage(500));

        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(discoverFetcher).fetch(captor.capture());
        assertEquals(500, captor.getValue().page());
    }

    /**
     * Page at the TMDB cap still calls TMDB for TV.
     */
    @Test
    void pageAtMaxTvCallsTmdb() {
        when(discoverCacheService.getCachedTvShows(any())).thenReturn(null);
        when(tvDiscoverFetcher.fetch(any())).thenReturn(List.<TvShow>of());
        when(tvShowMapper.toDtos(List.of())).thenReturn(List.of());

        tvShowService.discoverTvShows(requestWithPage(500));

        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(tvDiscoverFetcher).fetch(captor.capture());
        assertEquals(500, captor.getValue().page());
    }

    /**
     * Runtime set on movies skips cache and reaches the fetcher with runtime bounds.
     */
    @Test
    void runtimeSetMovieBypassesCacheAndCallsFetcher() {
        when(discoverCacheService.getCachedMovies(any())).thenReturn(null);
        when(discoverFetcher.fetch(any())).thenReturn(List.of());
        when(movieMapper.toDtos(List.of())).thenReturn(List.of());

        movieService.discoverMovies(requestWithRuntime(60, 120));

        verify(discoverCacheService).getCachedMovies(any());
        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(discoverFetcher).fetch(captor.capture());
        assertEquals(60, captor.getValue().minRuntime());
        assertEquals(120, captor.getValue().maxRuntime());
    }

    /**
     * Runtime set on TV skips cache and reaches the fetcher with runtime bounds.
     */
    @Test
    void runtimeSetTvBypassesCacheAndCallsFetcher() {
        when(discoverCacheService.getCachedTvShows(any())).thenReturn(null);
        when(tvDiscoverFetcher.fetch(any())).thenReturn(List.of());
        when(tvShowMapper.toDtos(List.of())).thenReturn(List.of());

        tvShowService.discoverTvShows(requestWithRuntime(60, 120));

        verify(discoverCacheService).getCachedTvShows(any());
        ArgumentCaptor<DiscoverRequest> captor = ArgumentCaptor.forClass(DiscoverRequest.class);
        verify(tvDiscoverFetcher).fetch(captor.capture());
        assertEquals(60, captor.getValue().minRuntime());
        assertEquals(120, captor.getValue().maxRuntime());
    }

    /**
     * Runtime unset keeps the default movie path on cache with zero TMDB calls.
     */
    @Test
    void runtimeUnsetMovieUsesCacheWithoutTmdb() {
        MovieDto dto = sampleMovieDto();
        when(discoverCacheService.getCachedMovies(any())).thenReturn(List.of(dto));

        List<MovieDto> result = movieService.discoverMovies(requestWithRuntime(null, null));

        assertEquals(List.of(dto), result);
        verify(discoverFetcher, never()).fetch(any());
        verify(tmdbClient, never()).discoverMovies(any());
    }

    private static DiscoverRequest requestWithVoteCount(Integer voteCount) {
        return new DiscoverRequest(null, null, voteCount, null, null, null, null, null, null, null, null, null, null);
    }

    private static DiscoverRequest requestWithPage(int page) {
        return new DiscoverRequest(null, null, null, null, page, null, null, null, null, null, null, null, null);
    }

    private static DiscoverRequest requestWithRuntime(Integer minRuntime, Integer maxRuntime) {
        return new DiscoverRequest(null, null, null, null, null, null, null, null, null, minRuntime, maxRuntime, null, null);
    }

    private static MovieDto sampleMovieDto() {
        return new MovieDto(
                1, false, null, List.of(), "en", "Title", "overview", 1.0, null, null, "Title", false, 7.0, 100);
    }

    private static TvShowDto sampleTvDto() {
        return new TvShowDto(
                1, false, null, List.of(), List.of(), "en", "Name", "overview", 1.0, null, null, "Name", 7.0, 100,
                null, null, null, null, null, null, List.of(), List.of(), null, List.of());
    }
}
