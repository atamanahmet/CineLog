package com.atamanahmet.cinelog.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Username can not be empty")
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Not unique: two users can end up with the same password hash, that is fine.
     */
    @NotBlank(message = "Password can not be empty")
    @Column(nullable = false)
    private String password;

    @Email
    @Size(max = 254)
    @Column(nullable = true, unique = true, length = 254)
    private String email;

    /**
     * Secure Cloudinary URL for the profile photo, or null when unset.
     */
    @Column(name = "profile_picture_url", nullable = true, length = 1024)
    private String profilePictureUrl;

    @Embedded
    private RecommendationPreferences recommendationPreferences = RecommendationPreferences.defaults();

    @Setter(AccessLevel.NONE)
    @ElementCollection
    @CollectionTable(name = "user_recommendation", joinColumns = @JoinColumn(name = "user_id"))
    private List<MediaKey> recommendation = new ArrayList<>();

    /**
     * Clear and refill this user's recommendation keys for one media type when they changed.
     */
    public void replaceRecommendation(TmdbMediaType mediaType, List<MediaKey> keys) {
        List<MediaKey> incoming = (keys == null ? List.<MediaKey>of() : keys).stream()
                .filter(key -> key.mediaType() == mediaType)
                .toList();
        List<MediaKey> currentOfType = recommendation.stream()
                .filter(key -> key.mediaType() == mediaType)
                .toList();
        if (currentOfType.equals(incoming)) {
            return;
        }
        List<MediaKey> next = new ArrayList<>();
        for (MediaKey existing : recommendation) {
            if (existing.mediaType() != mediaType) {
                next.add(existing);
            }
        }
        next.addAll(incoming);
        recommendation.clear();
        recommendation.addAll(next);
    }

}
