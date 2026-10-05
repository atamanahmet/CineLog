package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.repository.MediaKeyView;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class RecommendationExclusionServiceTest {

    @Mock
    private UserListEntryRepository userListEntryRepository;

    private RecommendationLimitsProperties limits;
    private RecommendationExclusionService service;
    private ListAppender<ILoggingEvent> appender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        limits = new RecommendationLimitsProperties();
        limits.setMaxExclude(10000);
        service = new RecommendationExclusionService(userListEntryRepository, limits);

        Logger logger = (Logger) LoggerFactory.getLogger(RecommendationExclusionService.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.WARN);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(RecommendationExclusionService.class);
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
    }

    /**
     * forEngine sorts by tmdb_id then media type.
     */
    @Test
    void forEngineSortsByTmdbIdThenMediaType() {
        when(userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                eq(7), any(), eq(TmdbMediaType.MOVIE)))
                .thenReturn(List.of(
                        view(30, TmdbMediaType.MOVIE),
                        view(10, TmdbMediaType.MOVIE),
                        view(20, TmdbMediaType.MOVIE)));

        List<RecommendationLovedPayload> result = service.forEngine(7, TmdbMediaType.MOVIE);

        assertThat(result).containsExactly(
                new RecommendationLovedPayload(10, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(20, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(30, TmdbMediaType.MOVIE));
    }

    /**
     * Cap truncates to maxExclude and logs one WARN with total and sent.
     */
    @Test
    void forEngineCutsAtMaxExcludeAndWarnsOnce() {
        limits.setMaxExclude(3);
        when(userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                eq(7), any(), eq(TmdbMediaType.MOVIE)))
                .thenReturn(List.of(
                        view(5, TmdbMediaType.MOVIE),
                        view(4, TmdbMediaType.MOVIE),
                        view(3, TmdbMediaType.MOVIE),
                        view(2, TmdbMediaType.MOVIE),
                        view(1, TmdbMediaType.MOVIE)));

        List<RecommendationLovedPayload> result = service.forEngine(7, TmdbMediaType.MOVIE);

        assertThat(result).containsExactly(
                new RecommendationLovedPayload(1, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(2, TmdbMediaType.MOVIE),
                new RecommendationLovedPayload(3, TmdbMediaType.MOVIE));
        List<ILoggingEvent> warns = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();
        assertThat(warns).hasSize(1);
        assertThat(warns.get(0).getFormattedMessage())
                .contains("total=5")
                .contains("sent=3");
    }

    /**
     * Empty repository result yields an empty exclude list and no WARN.
     */
    @Test
    void forEngineEmptySetGivesEmptyList() {
        when(userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                eq(7), any(), eq(TmdbMediaType.MOVIE)))
                .thenReturn(List.of());

        assertThat(service.forEngine(7, TmdbMediaType.MOVIE)).isEmpty();
        assertThat(appender.list.stream().filter(event -> event.getLevel() == Level.WARN)).isEmpty();
    }

    /**
     * allExcluded returns both media types with no cap.
     */
    @Test
    void allExcludedReturnsBothTypesUncapped() {
        when(userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                eq(7), any(), isNull()))
                .thenReturn(List.of(
                        view(1, TmdbMediaType.MOVIE),
                        view(2, TmdbMediaType.TV)));

        assertThat(service.allExcluded(7)).isEqualTo(Set.of(
                new com.atamanahmet.cinelog.domain.entity.MediaKey(1, TmdbMediaType.MOVIE),
                new com.atamanahmet.cinelog.domain.entity.MediaKey(2, TmdbMediaType.TV)));
    }

    private static MediaKeyView view(int tmdbId, TmdbMediaType mediaType) {
        return new MediaKeyView() {
            @Override
            public Integer getTmdbId() {
                return tmdbId;
            }

            @Override
            public TmdbMediaType getMediaType() {
                return mediaType;
            }
        };
    }
}
