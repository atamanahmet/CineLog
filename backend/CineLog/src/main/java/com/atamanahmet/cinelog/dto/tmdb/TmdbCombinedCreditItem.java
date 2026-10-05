package com.atamanahmet.cinelog.dto.tmdb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCombinedCreditItem {

    private Integer id;

    @JsonProperty("media_type")
    private String mediaType;

    private String title;

    private String name;

    @JsonProperty("poster_path")
    private String posterPath;

    private String character;

    private String job;

    private String department;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    @JsonProperty("vote_average")
    private Double voteAverage;

    private Double popularity;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    @JsonProperty("episode_count")
    private Integer episodeCount;
}
