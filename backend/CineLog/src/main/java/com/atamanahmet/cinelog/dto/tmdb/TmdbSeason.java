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
public class TmdbSeason {

    @JsonProperty("season_number")
    private Integer seasonNumber;

    private String name;

    @JsonProperty("episode_count")
    private Integer episodeCount;

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("vote_average")
    private Double voteAverage;
}
