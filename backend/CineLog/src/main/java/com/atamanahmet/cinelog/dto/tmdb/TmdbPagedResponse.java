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
public class TmdbPagedResponse<T> {

    private Integer page;

    private List<T> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;
}
