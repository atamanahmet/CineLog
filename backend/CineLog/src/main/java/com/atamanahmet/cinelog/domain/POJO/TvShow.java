package com.atamanahmet.cinelog.domain.POJO;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.atamanahmet.cinelog.converter.CastJsonConverter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tvShow")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TvShow {

    @Id
    @Column
    private Integer id;

    @Column
    private Boolean adult = true;

    @Column
    private String backdrop_path;

    @ElementCollection
    @Column
    private List<Integer> genre_ids;

    @ElementCollection
    @Column
    private List<String> origin_country;

    @Column
    private String original_language;

    @Column
    @JsonProperty("original_name")
    private String original_title;

    @Column(length = 2048)
    private String overview;

    @Column
    private Double popularity;

    @Column
    private String poster_path;

    @Column
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String first_air_date;

    @JsonProperty("name")
    @Column
    private String title;

    @Column
    private Double vote_average;

    @Column
    private Integer vote_count;

    @Column
    private String trailer_path;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cast_data", columnDefinition = "jsonb")
    @Convert(converter = CastJsonConverter.class)
    private Cast cast;
}
