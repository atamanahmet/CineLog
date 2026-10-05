package com.atamanahmet.cinelog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.service.RecommendationExclusionService;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserListEntryRepositoryMediaKeyProjectionTest {

    @Autowired
    private UserListEntryRepository userListEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Projection returns WATCHED, REJECTED, LOVED only, distinct, per user, and filters media type.
     */
    @Test
    void projectionReturnsExclusionKeysOnlyDistinctPerUser() {
        User alice = saveUser("proj-alice");
        User bob = saveUser("proj-bob");

        saveEntry(alice.getId(), 10, TmdbMediaType.MOVIE, ListType.WATCHED);
        saveEntry(alice.getId(), 20, TmdbMediaType.MOVIE, ListType.REJECTED);
        saveEntry(alice.getId(), 30, TmdbMediaType.TV, ListType.LOVED);
        saveEntry(alice.getId(), 40, TmdbMediaType.MOVIE, ListType.WATCHLIST);
        saveEntry(alice.getId(), 10, TmdbMediaType.MOVIE, ListType.LOVED);
        saveEntry(bob.getId(), 99, TmdbMediaType.MOVIE, ListType.WATCHED);
        entityManager.flush();

        List<MediaKeyView> all = userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                alice.getId(), RecommendationExclusionService.EXCLUSION_LIST_TYPES, null);
        assertThat(all).extracting(MediaKeyView::getTmdbId)
                .containsExactlyInAnyOrder(10, 20, 30);
        assertThat(all).extracting(MediaKeyView::getMediaType)
                .containsExactlyInAnyOrder(TmdbMediaType.MOVIE, TmdbMediaType.MOVIE, TmdbMediaType.TV);
        assertThat(all).noneMatch(view -> view.getTmdbId() == 40);
        assertThat(all).noneMatch(view -> view.getTmdbId() == 99);

        List<MediaKeyView> movies = userListEntryRepository.findDistinctMediaKeysByUserIdAndListTypeIn(
                alice.getId(), RecommendationExclusionService.EXCLUSION_LIST_TYPES, TmdbMediaType.MOVIE);
        assertThat(movies).extracting(MediaKeyView::getTmdbId)
                .containsExactlyInAnyOrder(10, 20);
        assertThat(movies).allMatch(view -> view.getMediaType() == TmdbMediaType.MOVIE);
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password-long-enough");
        return userRepository.save(user);
    }

    private void saveEntry(Integer userId, int tmdbId, TmdbMediaType mediaType, ListType listType) {
        UserListEntry entry = new UserListEntry();
        entry.setUserId(userId);
        entry.setTmdbId(tmdbId);
        entry.setMediaType(mediaType);
        entry.setListType(listType);
        userListEntryRepository.save(entry);
    }
}
