package com.atamanahmet.cinelog.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Normalized list membership for a user (watchlist / watched / loved).
 */
@Entity
@Table(
        name = "user_list_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_list_entry",
                columnNames = { "user_id", "tmdb_id", "media_type", "list_type" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @NotNull
    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private TmdbMediaType mediaType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 16)
    private ListType listType;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
