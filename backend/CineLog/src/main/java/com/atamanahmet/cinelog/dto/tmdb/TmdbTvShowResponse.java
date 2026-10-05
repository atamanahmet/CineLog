package com.atamanahmet.cinelog.dto.tmdb;

import java.util.List;

import com.atamanahmet.cinelog.dto.MovieVideoDTO;
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
public class TmdbTvShowResponse {

    private Integer id;

    private Boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    private List<TmdbGenre> genres;

    @JsonProperty("origin_country")
    private List<String> originCountry;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_name")
    private String originalTitle;

    private String overview;

    private Double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("first_air_date")
    private String releaseDate;

    @JsonProperty("name")
    private String title;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private String status;

    @JsonProperty("number_of_seasons")
    private Integer numberOfSeasons;

    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;

    @JsonProperty("episode_run_time")
    private List<Integer> episodeRunTime;

    @JsonProperty("last_air_date")
    private String lastAirDate;

    private List<TmdbNetwork> networks;

    @JsonProperty("created_by")
    private List<TmdbCreatedBy> createdBy;

    @JsonProperty("next_episode_to_air")
    private TmdbNextEpisode nextEpisodeToAir;

    private List<TmdbSeason> seasons;

    private TmdbCast credits;

    private MovieVideoDTO videos;
}
