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
public class TmdbPersonResponse {

    private Integer id;

    private String name;

    private String biography;

    @JsonProperty("profile_path")
    private String profilePath;

    private String birthday;

    private String deathday;

    @JsonProperty("place_of_birth")
    private String placeOfBirth;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    @JsonProperty("also_known_as")
    private List<String> alsoKnownAs;

    @JsonProperty("combined_credits")
    private TmdbCombinedCreditsResponse combinedCredits;
}
