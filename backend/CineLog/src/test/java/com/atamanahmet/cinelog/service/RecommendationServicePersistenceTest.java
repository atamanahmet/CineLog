package com.atamanahmet.cinelog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.atamanahmet.cinelog.client.RecommendationClient;
import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.config.RecommendationQualityProperties;
import com.atamanahmet.cinelog.domain.entity.CatalogCache;
import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.mapper.UserMapper;
import com.atamanahmet.cinelog.repository.CatalogCacheRepository;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.impl.UserServiceImpl;

import jakarta.persistence.EntityManager;

@DataJpaTest
@EnableConfigurationProperties({
        RecommendationLimitsProperties.class,
        RecommendationQualityProperties.class
})
@Import({
        UserServiceImpl.class,
        ListItemHydrator.class,
        RecommendationService.class,
        RecommendationExclusionService.class,
        RecommendationHydrator.class,
        RecommendationQualityFilter.class,
        CacheConfig.class
})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "recommendation.limits.max-results=100",
        "recommendation.limits.max-exclude=10000"
})
class RecommendationServicePersistenceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserListEntryRepository userListEntryRepository;

    @Autowired
    private CatalogCacheRepository catalogCacheRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private RecommendationClient recommendationClient;

    @MockitoBean(name = "tmdbHydrateClient")
    private TmdbClient tmdbClient;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private TvShowService tvShowService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CurrentUserService currentUserService;

    /**
     * Persisting recommendation keys on a managed User must survive flush and reload.
     */
    @Test
    void getRecommendationPersistsHitKeysOnUser() {
        User saved = new User();
        saved.setUsername("rec-user");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);

        UserListEntry loved = new UserListEntry();
        loved.setUserId(saved.getId());
        loved.setTmdbId(42);
        loved.setMediaType(TmdbMediaType.MOVIE);
        loved.setListType(ListType.LOVED);
        userListEntryRepository.save(loved);
        entityManager.flush();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);

        List<RecommendationHitPayload> hits = List.of(
                new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91),
                new RecommendationHitPayload(202, TmdbMediaType.MOVIE, 0.80));
        when(recommendationClient.requestRecommendations(any())).thenReturn(hits);

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(
                List.of(
                        new MediaKey(101, TmdbMediaType.MOVIE),
                        new MediaKey(202, TmdbMediaType.MOVIE)),
                reloaded.getRecommendation());
    }

    /**
     * Fetching MOVIE replaces only movie keys and keeps stored TV keys.
     */
    @Test
    void fetchingMovieDoesNotRemoveStoredTvKeys() {
        User saved = new User();
        saved.setUsername("movie-keep-tv");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);
        saved.replaceRecommendation(TmdbMediaType.TV, List.of(new MediaKey(55, TmdbMediaType.TV)));
        entityManager.flush();

        UserListEntry loved = new UserListEntry();
        loved.setUserId(saved.getId());
        loved.setTmdbId(42);
        loved.setMediaType(TmdbMediaType.MOVIE);
        loved.setListType(ListType.LOVED);
        userListEntryRepository.save(loved);
        entityManager.flush();
        entityManager.clear();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of(
                new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91)));

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(
                List.of(
                        new MediaKey(55, TmdbMediaType.TV),
                        new MediaKey(101, TmdbMediaType.MOVIE)),
                reloaded.getRecommendation());
    }

    /**
     * Fetching TV replaces only TV keys and keeps stored movie keys.
     */
    @Test
    void fetchingTvDoesNotRemoveStoredMovieKeys() {
        User saved = new User();
        saved.setUsername("tv-keep-movie");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);
        saved.replaceRecommendation(TmdbMediaType.MOVIE, List.of(new MediaKey(44, TmdbMediaType.MOVIE)));
        entityManager.flush();

        UserListEntry loved = new UserListEntry();
        loved.setUserId(saved.getId());
        loved.setTmdbId(42);
        loved.setMediaType(TmdbMediaType.TV);
        loved.setListType(ListType.LOVED);
        userListEntryRepository.save(loved);
        entityManager.flush();
        entityManager.clear();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of(
                new RecommendationHitPayload(202, TmdbMediaType.TV, 0.80)));

        recommendationService.getRecommendation(TmdbMediaType.TV, null, null);

        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(
                List.of(
                        new MediaKey(44, TmdbMediaType.MOVIE),
                        new MediaKey(202, TmdbMediaType.TV)),
                reloaded.getRecommendation());
    }

    /**
     * Real catalog_cache hydrate keeps engine order and scores, and skips missing keys.
     */
    @Test
    void getRecommendationHydratesFromRealCatalogCacheKeepingOrderAndScores() {
        User saved = new User();
        saved.setUsername("hydrate-user");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);

        UserListEntry loved = new UserListEntry();
        loved.setUserId(saved.getId());
        loved.setTmdbId(42);
        loved.setMediaType(TmdbMediaType.MOVIE);
        loved.setListType(ListType.LOVED);
        userListEntryRepository.save(loved);

        CatalogCache presentMovie = new CatalogCache();
        presentMovie.setTmdbId(99);
        presentMovie.setMediaType(TmdbMediaType.MOVIE);
        presentMovie.setTitle("Heat");
        catalogCacheRepository.save(presentMovie);

        CatalogCache presentTv = new CatalogCache();
        presentTv.setTmdbId(98);
        presentTv.setMediaType(TmdbMediaType.TV);
        presentTv.setTitle("Show");
        catalogCacheRepository.save(presentTv);
        entityManager.flush();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of(
                new RecommendationHitPayload(100, TmdbMediaType.MOVIE, 0.95),
                new RecommendationHitPayload(99, TmdbMediaType.MOVIE, 0.91),
                new RecommendationHitPayload(98, TmdbMediaType.TV, 0.80)));

        List<RecommendationItemDTO> items = recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        assertEquals(2, items.size());
        assertEquals(99, items.get(0).id());
        assertEquals(0.91, items.get(0).score());
        assertEquals(98, items.get(1).id());
        assertEquals(0.80, items.get(1).score());
    }

    /**
     * Empty loved list clears previously stored recommendation keys of both types.
     */
    @Test
    void emptyLovedListClearsStoredKeys() {
        User saved = new User();
        saved.setUsername("clear-user");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);
        saved.replaceRecommendation(TmdbMediaType.MOVIE, List.of(new MediaKey(55, TmdbMediaType.MOVIE)));
        saved.replaceRecommendation(TmdbMediaType.TV, List.of(new MediaKey(66, TmdbMediaType.TV)));
        entityManager.flush();
        entityManager.clear();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);

        recommendationService.getRecommendation(TmdbMediaType.MOVIE, null, null);

        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(List.of(), reloaded.getRecommendation());
    }

    /**
     * Seeded similar flow must not change the user's stored recommendation keys.
     */
    @Test
    void recommendFromSeedsLeavesStoredRecommendationUnchanged() {
        User saved = new User();
        saved.setUsername("similar-no-write");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);
        List<MediaKey> stored = List.of(
                new MediaKey(55, TmdbMediaType.MOVIE),
                new MediaKey(66, TmdbMediaType.TV));
        saved.replaceRecommendation(TmdbMediaType.MOVIE, List.of(new MediaKey(55, TmdbMediaType.MOVIE)));
        saved.replaceRecommendation(TmdbMediaType.TV, List.of(new MediaKey(66, TmdbMediaType.TV)));
        entityManager.flush();
        entityManager.clear();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of(
                new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91),
                new RecommendationHitPayload(202, TmdbMediaType.MOVIE, 0.80)));

        recommendationService.recommendFromSeeds(
                List.of(new RecommendationLovedPayload(42, TmdbMediaType.MOVIE)),
                TmdbMediaType.MOVIE,
                20,
                managed.getId(),
                null,
                null);

        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(stored, reloaded.getRecommendation());
    }

    /**
     * Filtered GET returns hits but leaves stored recommendation keys unchanged.
     */
    @Test
    void filteredGetDoesNotPersistRecommendationKeys() {
        User saved = new User();
        saved.setUsername("filtered-no-write");
        saved.setPassword("hashed");
        saved = userRepository.save(saved);
        List<MediaKey> stored = List.of(new MediaKey(55, TmdbMediaType.MOVIE));
        saved.replaceRecommendation(TmdbMediaType.MOVIE, stored);
        entityManager.flush();

        UserListEntry loved = new UserListEntry();
        loved.setUserId(saved.getId());
        loved.setTmdbId(42);
        loved.setMediaType(TmdbMediaType.MOVIE);
        loved.setListType(ListType.LOVED);
        userListEntryRepository.save(loved);
        entityManager.flush();
        entityManager.clear();

        User managed = userRepository.findById(saved.getId()).orElseThrow();
        when(currentUserService.getCurrentUser()).thenReturn(managed);
        when(recommendationClient.requestRecommendations(any())).thenReturn(List.of(
                new RecommendationHitPayload(101, TmdbMediaType.MOVIE, 0.91),
                new RecommendationHitPayload(202, TmdbMediaType.MOVIE, 0.80)));

        List<RecommendationItemDTO> items = recommendationService.getRecommendation(
                TmdbMediaType.MOVIE, null, List.of(16));

        assertEquals(0, items.size());
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals(stored, reloaded.getRecommendation());
    }
}
