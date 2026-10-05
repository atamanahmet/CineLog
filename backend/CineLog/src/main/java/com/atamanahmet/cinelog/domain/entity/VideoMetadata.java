package com.atamanahmet.cinelog.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
    @JsonProperty("iso_639_1")
    private String languageCode;

    @JsonProperty("iso_3166_1")
    private String countryCode;

    private String name;
    private String key;
    private String site;
    private int size;
    private String type;
    private boolean official;

    @JsonProperty("published_at")
    private String publishedAt;

    private String id;

}
