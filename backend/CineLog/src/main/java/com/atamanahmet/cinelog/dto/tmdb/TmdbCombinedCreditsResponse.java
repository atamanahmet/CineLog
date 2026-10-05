package com.atamanahmet.cinelog.dto.tmdb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCombinedCreditsResponse {

    private Integer id;

    private List<TmdbCombinedCreditItem> cast;

    private List<TmdbCombinedCreditItem> crew;
}
