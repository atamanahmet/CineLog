package com.atamanahmet.cinelog.client.tmdb;

import java.util.Optional;

import com.atamanahmet.cinelog.dto.MovieVideoDTO;
import com.atamanahmet.cinelog.domain.entity.VideoMetadata;

public final class TmdbTrailerUrls {

    private static final String YOUTUBE_WATCH = "https://www.youtube.com/watch?v=";

    private TmdbTrailerUrls() {
    }

    /**
     * Return the first YouTube trailer watch URL, if TMDB sent one.
     */
    public static Optional<String> firstYoutubeTrailer(MovieVideoDTO dto) {
        if (dto == null || dto.getResults() == null) {
            return Optional.empty();
        }
        for (VideoMetadata video : dto.getResults()) {
            if (video.getType() != null && video.getSite() != null
                    && "trailer".equals(video.getType().toLowerCase())
                    && "youtube".equals(video.getSite().toLowerCase())) {
                return Optional.of(YOUTUBE_WATCH + video.getKey());
            }
        }
        return Optional.empty();
    }
}
