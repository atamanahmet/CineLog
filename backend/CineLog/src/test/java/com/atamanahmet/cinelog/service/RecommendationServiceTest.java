package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.lenient;

import com.atamanahmet.cinelog.client.RecommendationClient;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.RecommendationPreferences;
import com.atamanahmet.cinelog.domain.entity.RecommendationScope;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.dto.RecommendationRequestPayload;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @Mock
    private RecommendationClient recommendationClient;

    @Mock
    private RecommendationHydrator recommendationHydrator;

    @Mock
    private RecommendationLimitsProperties recommendationLimitsProperties;

    @Mock
    private RecommendationExclusionService recommendationExclusionService;

    @InjectMocks
    private RecommendationService recommendationService;

    @BeforeEach
    void stubDefaultCap() {
        lenient().when(recommendationLimitsProperties.getMaxResults()).thenReturn(100);
        lenient().when(recommendationExclusionService.forEngine(any(), any())).thenReturn(List.of());
    }

    /**
     * Empty loved list clears stored keys of both types and skips the engine.
     */
    @Test
    void emptyLovedListClearsStoredKeys() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of());

        List<RecommendationItemDTO> items = recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        assertThat(items).isEmpty();
        verify(userService).replaceRecommendation(7, TmdbMediaType.MOVIE, List.of());
        verify(userService).replaceRecommendation(7, TmdbMediaType.TV, List.of());
        verify(recommendationClient, org.mockito.Mockito.never()).requestRecommendations(any());
    }

    /**
     * Engine request carries media_type and the full loved list of both types.
     */
    @Test
    void engineRequestCarriesMediaTypeAndFullLovedList() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(
                new MediaKey(42, TmdbMediaType.MOVIE),
                new MediaKey(7, TmdbMediaType.TV)));
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        RecommendationRequestPayload payload = captor.getValue();
        assertThat(payload.mediaType()).isEqualTo(TmdbMediaType.MOVIE);
        assertThat(payload.loved()).containsExactlyInAnyOrder(
                new RecommendationLovedPayload(42, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(7, TmdbMediaType.TV));
        verify(userService).replaceRecommendation(
                eq(7),
                eq(TmdbMediaType.MOVIE),
                eq(List.of(new MediaKey(99, TmdbMediaType.MOVIE))));
    }

    /**
     * Engine call runs outside an active Spring transaction.
     */
    @Test
    void engineCallRunsOutsideTransaction() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));

        AtomicBoolean engineSawTx = new AtomicBoolean(true);
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenAnswer(invocation -> {
                    engineSawTx.set(TransactionSynchronizationManager.isActualTransactionActive());
                    return List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91));
                });
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        assertThat(engineSawTx.get()).isFalse();
        verify(userService).replaceRecommendation(
                eq(7),
                eq(TmdbMediaType.MOVIE),
                eq(List.of(new MediaKey(99, TmdbMediaType.MOVIE))));
    }

    /**
     * Seeded similar flow sends request seeds to the engine, not the user's loved list.
     */
    @Test
    void recommendFromSeedsSendsRequestSeedsNotLovedList() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setRecommendationPreferences(RecommendationPreferences.defaults());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        List<RecommendationLovedPayload> seeds = List.of(
                new RecommendationLovedPayload(10, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(20, TmdbMediaType.TV));
        recommendationService.recommendFromSeeds(seeds, TmdbMediaType.MOVIE, 25, 7, null, null);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        RecommendationRequestPayload payload = captor.getValue();
        assertThat(payload.loved()).containsExactly(
                new RecommendationLovedPayload(10, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(20, TmdbMediaType.TV));
        assertThat(payload.mediaType()).isEqualTo(TmdbMediaType.MOVIE);
        assertThat(payload.limit()).isEqualTo(25);
        assertThat(payload.exclude()).isNull();
        verify(recommendationExclusionService).forEngine(7, TmdbMediaType.MOVIE);
        verify(userService, never()).findLovedKeys(any());
        verify(userService, never()).replaceRecommendation(any(), any(), any());
    }

    /**
     * Loved flow passes user id into forEngine and puts the result on the payload.
     */
    @Test
    void lovedFlowSendsForEngineExclude() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        when(recommendationExclusionService.forEngine(7, TmdbMediaType.MOVIE))
                .thenReturn(List.of(new RecommendationLovedPayload(99, TmdbMediaType.MOVIE)));
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationExclusionService).forEngine(7, TmdbMediaType.MOVIE);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().exclude()).containsExactly(
                new RecommendationLovedPayload(99, TmdbMediaType.MOVIE));
    }

    /**
     * Find-similar flow passes user id into forEngine and puts the result on the payload.
     */
    @Test
    void findSimilarFlowSendsForEngineExclude() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setRecommendationPreferences(RecommendationPreferences.defaults());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(recommendationExclusionService.forEngine(7, TmdbMediaType.MOVIE))
                .thenReturn(List.of(new RecommendationLovedPayload(55, TmdbMediaType.MOVIE)));
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.findSimilar(
                List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                TmdbMediaType.MOVIE,
                null,
                null);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationExclusionService).forEngine(7, TmdbMediaType.MOVIE);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().exclude()).containsExactly(
                new RecommendationLovedPayload(55, TmdbMediaType.MOVIE));
    }

    /**
     * Loved flow clamps stored maxResults to the configured cap before calling the engine.
     */
    @Test
    void lovedFlowClampsLimitToConfiguredCap() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setRecommendationPreferences(
                new RecommendationPreferences(RecommendationScope.ALL, 0.3, 100));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        when(recommendationLimitsProperties.getMaxResults()).thenReturn(50);
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().limit()).isEqualTo(50);
    }

    /**
     * Find similar flow clamps stored maxResults to the configured cap before calling the engine.
     */
    @Test
    void findSimilarFlowClampsLimitToConfiguredCap() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setRecommendationPreferences(
                new RecommendationPreferences(RecommendationScope.ALL, 0.3, 100));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(recommendationLimitsProperties.getMaxResults()).thenReturn(50);
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.findSimilar(
                List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                TmdbMediaType.MOVIE,
                null,
                null);

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().limit()).isEqualTo(50);
    }

    /**
     * Empty genre lists are omitted from the engine payload.
     */
    @Test
    void emptyGenreListsOmittedFromPayload() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, List.of(), List.of());

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().genreInclude()).isNull();
        assertThat(captor.getValue().genreExclude()).isNull();
    }

    /**
     * Non-empty genre lists are sent on the engine payload.
     */
    @Test
    void genreListsCarriedOnPayload() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, List.of(878, 28), List.of(16));

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().genreInclude()).containsExactly(878, 28);
        assertThat(captor.getValue().genreExclude()).containsExactly(16);
        verify(userService, never()).replaceRecommendation(any(), any(), any());
    }

    /**
     * Filtered GET must not overwrite stored recommendation keys.
     */
    @Test
    void filteredRecommendationSkipsPersistence() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userService.findLovedKeys(7)).thenReturn(Set.of(new MediaKey(42, TmdbMediaType.MOVIE)));
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, List.of(16));

        verify(userService, never()).replaceRecommendation(any(), any(), any());
    }

    /**
     * Find-similar passes genre lists to the engine.
     */
    @Test
    void findSimilarPassesGenreLists() {
        User user = new User();
        user.setId(7);
        user.setUsername("alice");
        user.setRecommendationPreferences(RecommendationPreferences.defaults());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(recommendationClient.requestRecommendations(any(RecommendationRequestPayload.class)))
                .thenReturn(List.of(new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91)));
        when(recommendationHydrator.hydrate(any(), any())).thenReturn(List.of());

        recommendationService.findSimilar(
                List.of(new RecommendationLovedPayload(10, TmdbMediaType.MOVIE)),
                TmdbMediaType.MOVIE,
                List.of(878),
                List.of());

        ArgumentCaptor<RecommendationRequestPayload> captor =
                ArgumentCaptor.forClass(RecommendationRequestPayload.class);
        verify(recommendationClient).requestRecommendations(captor.capture());
        assertThat(captor.getValue().genreInclude()).containsExactly(878);
        assertThat(captor.getValue().genreExclude()).isNull();
    }
}
