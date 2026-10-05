package com.atamanahmet.cinelog.domain.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cached TMDB display data. PK is (tmdbId, mediaType) because movie and TV IDs share namespaces.
 */
@Entity
@Table(name = "catalog_cache")
@IdClass(CatalogCacheId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCache {

    @Id
    @NotNull
    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @Id
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private TmdbMediaType mediaType;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    /**
     * Filled only when item is added to loved list; otherwise left null.
     */
    @Column(length = 2048)
    private String overview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
