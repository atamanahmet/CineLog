package com.atamanahmet.cinelog.domain.POJO;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.atamanahmet.cinelog.converter.CastJsonConverter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Movie {

    @Id
    @Column(nullable = false, unique = true)
    private Integer id;

    @Column
    private boolean adult;

    @Column
    private String backdrop_path;

    @ElementCollection
    @CollectionTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "genre_id")
    private List<Integer> genre_ids = new ArrayList<>();

    @Column
    private String original_language;

    @Column
    private String original_title;

    @Column(length = 2048)
    private String overview;

    @Column
    private double popularity;

    @Column
    private String poster_path;

    @Column
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date release_date;

    @Column
    private String title;

    @Column
    private boolean video;

    @Column
    private double vote_average;

    @Column
    private int vote_count;

    @Column
    private String trailer_path;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cast_data", columnDefinition = "jsonb")
    @Convert(converter = CastJsonConverter.class)
    private Cast cast;
}
