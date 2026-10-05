package com.atamanahmet.cinelog.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.client.RecommendationClient;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.RecommendationPreferences;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.RecommendationHitPayload;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.dto.RecommendationRequestPayload;
import com.atamanahmet.cinelog.exception.UnauthorizedActionException;
import com.atamanahmet.cinelog.exception.UserNotFoundException;
import com.atamanahmet.cinelog.validation.GenreFilterValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final RecommendationClient recommendationClient;
    private final RecommendationHydrator recommendationHydrator;
    private final RecommendationLimitsProperties recommendationLimitsProperties;
    private final RecommendationExclusionService recommendationExclusionService;

    /**
     * Ask the rec engine for one media type, persist that type's keys when unfiltered, and return hydrated rows.
     */
    public List<RecommendationItemDTO> getRecommendation(
            TmdbMediaType mediaType,
            List<Integer> genreInclude,
            List<Integer> genreExclude) {
        GenreFilterValidator.validate(genreInclude, genreExclude);
        List<Integer> include = GenreFilterValidator.normalize(genreInclude);
        List<Integer> excludeGenres = GenreFilterValidator.normalize(genreExclude);

        User user;
        try {
            user = currentUserService.getCurrentUser();
        } catch (UnauthorizedActionException | UserNotFoundException e) {
            log.info("User not exist");
            return List.of();
        }

        Set<MediaKey> lovedKeys = userService.findLovedKeys(user.getId());
        boolean filtered = GenreFilterValidator.isFiltered(include, excludeGenres);
        if (lovedKeys.isEmpty()) {
            if (!filtered) {
                userService.replaceRecommendation(user.getId(), TmdbMediaType.MOVIE, List.of());
                userService.replaceRecommendation(user.getId(), TmdbMediaType.TV, List.of());
            }
            return List.of();
        }

        List<RecommendationLovedPayload> lovedPayload = lovedKeys.stream()
                .map(key -> new RecommendationLovedPayload(key.tmdbId(), key.mediaType()))
                .toList();
        RecommendationPreferences stored = user.getRecommendationPreferences();
        RecommendationPreferences prefs = stored == null
                ? RecommendationPreferences.defaults()
                : stored.withDefaults();

        SeededRecommendation result = recommendFromSeeds(
                lovedPayload, mediaType, prefs.getMaxResults(), user.getId(), include, excludeGenres);
        if (!filtered) {
            userService.replaceRecommendation(user.getId(), mediaType, result.keys());
        }
        return result.items();
    }

    /**
     * Find similar titles from request seeds using the user's maxResults setting. Never writes.
     */
    public List<RecommendationItemDTO> findSimilar(
            List<RecommendationLovedPayload> seeds,
            TmdbMediaType mediaType,
            List<Integer> genreInclude,
            List<Integer> genreExclude) {
        GenreFilterValidator.validate(genreInclude, genreExclude);
        List<Integer> include = GenreFilterValidator.normalize(genreInclude);
        List<Integer> excludeGenres = GenreFilterValidator.normalize(genreExclude);

        User user = currentUserService.getCurrentUser();
        RecommendationPreferences stored = user.getRecommendationPreferences();
        RecommendationPreferences prefs = stored == null
                ? RecommendationPreferences.defaults()
                : stored.withDefaults();
        return recommendFromSeeds(
                seeds, mediaType, prefs.getMaxResults(), user.getId(), include, excludeGenres)
                .items();
    }

    /**
     * Call the engine with the given seeds and hydrate hits. Never writes recommendation state.
     */
    public SeededRecommendation recommendFromSeeds(
            List<RecommendationLovedPayload> seeds,
            TmdbMediaType mediaType,
            Integer limit,
            Integer userId,
            List<Integer> genreInclude,
            List<Integer> genreExclude) {
        User user = currentUserService.getCurrentUser();
        RecommendationPreferences stored = user.getRecommendationPreferences();
        Double minScore = stored == null ? null : stored.getMinScore();
        int effectiveLimit = clampEngineLimit(limit);
        List<RecommendationLovedPayload> exclude =
                recommendationExclusionService.forEngine(userId, mediaType);
        List<Integer> include = GenreFilterValidator.normalize(genreInclude);
        List<Integer> excludeGenres = GenreFilterValidator.normalize(genreExclude);
        List<RecommendationHitPayload> hits = recommendationClient.requestRecommendations(
                new RecommendationRequestPayload(
                        seeds,
                        mediaType,
                        effectiveLimit,
                        minScore,
                        exclude.isEmpty() ? null : exclude,
                        include.isEmpty() ? null : include,
                        excludeGenres.isEmpty() ? null : excludeGenres));
        List<MediaKey> recommendationKeys = hits.stream()
                .map(hit -> new MediaKey(hit.tmdbId(), hit.mediaType()))
                .toList();
        Map<MediaKey, Double> scoresByKey = hits.stream()
                .collect(Collectors.toMap(
                        hit -> new MediaKey(hit.tmdbId(), hit.mediaType()),
                        RecommendationHitPayload::score,
                        (left, right) -> left));
        List<RecommendationItemDTO> items = recommendationHydrator.hydrate(recommendationKeys, scoresByKey);
        return new SeededRecommendation(recommendationKeys, items);
    }

    /**
     * Cap the request limit at the configured operator max.
     */
    private int clampEngineLimit(Integer limit) {
        int stored = limit != null ? limit : RecommendationPreferences.DEFAULT_MAX_RESULTS;
        return Math.min(stored, recommendationLimitsProperties.getMaxResults());
    }

    /**
     * Engine hit keys plus hydrated response rows from one seeded request.
     */
    public record SeededRecommendation(List<MediaKey> keys, List<RecommendationItemDTO> items) {
    }
}
