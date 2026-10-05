package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atamanahmet.cinelog.client.RecommendationClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.RecommendationCacheProperties;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.dto.RecommendationRequestPayload;
import com.atamanahmet.cinelog.dto.tmdb.MediaDisplay;
import com.atamanahmet.cinelog.repository.CatalogCacheRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceMediaTypeSeparationTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @Mock
    private RecommendationClient recommendationClient;

    @Mock
    private CatalogCacheRepository catalogCacheRepository;

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private RecommendationExclusionService recommendationExclusionService;

    private Cache<MediaKey, MediaDisplay> displayCache;
    private ExecutorService executor;
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        displayCache = Caffeine.newBuilder().maximumSize(100).build();
        executor = Executors.newFixedThreadPool(2);
        RecommendationCacheProperties properties = new RecommendationCacheProperties();
        properties.setHydrateTimeoutSeconds(2);
        RecommendationQualityFilter qualityFilter =
                new RecommendationQualityFilter(new RecommendationQualityProperties());
        RecommendationHydrator hydrator = new RecommendationHydrator(
                catalogCacheRepository, tmdbClient, displayCache, executor, properties, qualityFilter);
        RecommendationLimitsProperties limits = new RecommendationLimitsProperties();
        org.mockito.Mockito.lenient()
                .when(recommendationExclusionService.forEngine(any(), any()))
                .thenReturn(List.of());
        recommendationService = new RecommendationService(
                currentUserService,
                userService,
                recommendationClient,
                hydrator,
                limits,
                recommendationExclusionService);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /**
     * MOVIE and TV engine calls return separate hydrated rows; loved list stays full both times.
     */
    @Test
    void movieAndTvReturnSeparateRecommendations() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(
                new MediaKey(42, TmdbMediaType.MOVIE),
                new MediaKey(7, TmdbMediaType.TV)));

        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenAnswer(invocation -> {
                    RecommendationRequestPayload payload = invocation.getArgument(0);
                    if (payload.mediaType() == TmdbMediaType.MOVIE) {
                        return List.of(
                                new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91),
                                new RecommendationHitPayload(102, TmdbMediaType.MOVIE, 0.80));
                    }
                    if (payload.mediaType() == TmdbMediaType.TV) {
                        return List.of(
                                new RecommendationHitPayload(201, TmdbMediaType.TV, 0.88),
                                new RecommendationHitPayload(202, TmdbMediaType.TV, 0.77));
                    }
                    return List.of();
                });

        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.MOVIE), anyCollection()))
                .thenReturn(List.of());
        when(catalogCacheRepository.findByMediaTypeAndTmdbIdIn(eq(TmdbMediaType.TV), anyCollection()))
                .thenReturn(List.of());
        when(tmdbClient.getMovieDisplayById(eq(101), any(Instant.class)))
                .thenReturn(new MediaDisplay(101, TmdbMediaType.MOVIE, "Movie A", null, null, null, 7.1, null, List.of()));
        when(tmdbClient.getMovieDisplayById(eq(102), any(Instant.class)))
                .thenReturn(new MediaDisplay(102, TmdbMediaType.MOVIE, "Movie B", null, null, null, 6.8, null, List.of()));
        when(tmdbClient.getTvDisplayById(eq(201), any(Instant.class)))
                .thenReturn(new MediaDisplay(201, TmdbMediaType.TV, "Show A", null, null, null, 8.0, null, List.of()));
        when(tmdbClient.getTvDisplayById(eq(202), any(Instant.class)))
                .thenReturn(new MediaDisplay(202, TmdbMediaType.TV, "Show B", null, null, null, 7.5, null, List.of()));

        List<RecommendationItemDTO> movieItems =
                recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);
        List<RecommendationItemDTO> tvItems =
                recommendationService.getRecommendation(TmdbMediaType.TV, null, null);

        assertThat(movieItems).extracting(RecommendationItemDTO::mediaType)
                .containsOnly(TmdbMediaType.MOVIE);
        assertThat(tvItems).extracting(RecommendationItemDTO::mediaType)
                .containsOnly(TmdbMediaType.TV);

        Set<Integer> movieIds = movieItems.stream()
                .map(RecommendationItemDTO::id)
                .collect(Collectors.toSet());
        Set<Integer> tvIds = tvItems.stream()
                .map(RecommendationItemDTO::id)
                .collect(Collectors.toSet());
        assertThat(movieIds).doesNotContainAnyElementsOf(tvIds);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient, org.mockito.Mockito.times(2)).requestRecommendations(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        for (RecommendationRequestPayload payload : captor.getAllValues()) {
            assertThat(payload.loved()).containsExactlyInAnyOrder(
                    new RecommendationLovedPayload(42, TmdbMediaType.MOVIE),
                    new RecommendationLovedPayload(7, TmdbMediaType.TV));
        }
        assertThat(captor.getAllValues())
                .extracting(RecommendationRequestPayload::mediaType)
                .containsExactly(TmdbMediaType.MOVIE, TmdbMediaType.TV);
    }
}
