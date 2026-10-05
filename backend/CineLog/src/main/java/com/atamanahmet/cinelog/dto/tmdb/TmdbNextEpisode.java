package com.atamanahmet.cinelog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbNextEpisode {

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("episode_number")
    private Integer episodeNumber;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    private String name;
}
