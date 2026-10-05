package com.atamanahmet.cinelog.domain.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tv_shows")
public class TvShow {

    @Id
    @Column
    private Integer id;

    @Column
    private Boolean adult;

    @Column(name = "backdrop_path")
    private String backdropPath;

    @ElementCollection
    @CollectionTable(name = "tv_show_genres", joinColumns = @JoinColumn(name = "tv_show_id"))
    @Column(name = "genre_id", nullable = false)
    private List<Integer> genreIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tv_show_origin_country", joinColumns = @JoinColumn(name = "tv_show_id"))
    @Column(name = "origin_country", nullable = false)
    private List<String> originCountry = new ArrayList<>();

    @Column(name = "original_language")
    private String originalLanguage;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(length = 2048)
    private String overview;

    @Column
    private Double popularity;

    @Column(name = "poster_path")
    private String posterPath;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column
    private String title;

    @Column(name = "vote_average")
    private Double voteAverage;

    @Column(name = "vote_count")
    private Integer voteCount;
}
