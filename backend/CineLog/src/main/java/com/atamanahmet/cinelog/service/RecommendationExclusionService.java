package com.atamanahmet.cinelog.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.repository.MediaKeyView;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Titles a user must not be recommended.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationExclusionService {

    public static final Set<ListType> EXCLUSION_LIST_TYPES = Set.of(
            ListType.WATCHED,
            ListType.REJECTED,
            ListType.LOVED);

    private final UserListEntryRepository userListEntryRepository;
    private final RecommendationLimitsProperties recommendationLimitsProperties;

    /**
     * All excluded keys for one user, both media types, uncapped.
     */
    @Transactional(readOnly = true)
    public Set<MediaKey> allExcluded(Integer userId) {
        return loadKeys(userId, null).stream()
                .map(view -> new MediaKey(view.getTmdbId(), view.getMediaType()))
                .collect(Collectors.toSet());
    }

    /**
     * Sorted exclude payload for the engine. Null mediaType means both types.
     * Truncates to maxExclude and logs one WARN when cut.
     */
    @Transactional(readOnly = true)
    public List<RecommendationLovedPayload> forEngine(Integer userId, TmdbMediaType mediaType) {
        List<RecommendationLovedPayload> sorted = loadKeys(userId, mediaType).stream()
                .map(view -> new RecommendationLovedPayload(view.getTmdbId(), view.getMediaType()))
                .sorted(Comparator
                        .comparing(RecommendationLovedPayload::tmdbId)
                        .thenComparing(payload -> payload.mediaType().name()))
                .toList();
        int maxExclude = recommendationLimitsProperties.getMaxExclude();
        if (sorted.size() <= maxExclude) {
            return sorted;
        }
        List<RecommendationLovedPayload> sent = sorted.subList(0, maxExclude);
        log.warn(
                "exclude list truncated userId={} total={} sent={}",
                userId,
                sorted.size(),
                sent.size());
        return List.copyOf(sent);
    }

    private List<MediaKeyView> loadKeys(Integer userId, TmdbMediaType mediaType) {
        return userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                userId, EXCLUSION_LIST_TYPES, mediaType);
    }
}
