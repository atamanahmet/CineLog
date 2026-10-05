package com.atamanahmet.cinelog.domain.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastMember {

    private Integer id;

    private String name;

    @JsonProperty("original_name")
    private String originalName;

    private boolean adult;

    private int gender;

    @JsonProperty("known_for_department")
    private String knownForDepartment; // Acting, Directing etc.

    private String job;

    private String character;

    @JsonProperty("credit_id")
    private String creditId;

    private int order;

    private double popularity;

    @JsonProperty("profile_path")
    private String profilePath;
}
