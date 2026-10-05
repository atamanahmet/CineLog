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
public class TmdbMovieResponse {

    private Integer id;

    private Boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    private List<TmdbGenre> genres;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;

    private Double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("release_date")
    private String releaseDate;

    private String title;

    private Boolean video;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private TmdbCast credits;

    private MovieVideoDTO videos;
}
