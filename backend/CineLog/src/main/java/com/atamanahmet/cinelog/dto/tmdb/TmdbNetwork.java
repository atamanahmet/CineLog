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
public class TmdbNetwork {

    private Integer id;

    private String name;

    @JsonProperty("logo_path")
    private String logoPath;

    @JsonProperty("origin_country")
    private String originCountry;
}
