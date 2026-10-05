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
public class TmdbCreatedBy {

    private Integer id;

    private String name;

    private Integer gender;

    @JsonProperty("profile_path")
    private String profilePath;
}
