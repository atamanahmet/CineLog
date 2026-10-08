package com.atamanahmet.cinelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.UserDetailsImpl;

import jakarta.persistence.EntityManager;

/**
 * Real PostgreSQL coverage for the add SQL. Runs only when CINELOG_PG_TEST is set and the
 * docker compose Postgres container holds a separate cinelog_test database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/cinelog_test",
        "spring.datasource.username=cinelog_test",
        "spring.datasource.password=cinelog_test",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "CINELOG_PG_TEST", matches = "(?i)true|1")
class UserListServiceAddPostgresTest {

    @Autowired
    private UserListEntryRepository userListEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private MovieService movieService;
    private TvShowService tvShowService;
    private UserListService userListService;

    @BeforeEach
    void setUp() {
        movieService = mock(MovieService.class);
        tvShowService = mock(TvShowService.class);
        userListService = new UserListService(
                userListEntryRepository,
                movieService,
                tvShowService,
                mock(CurrentUserService.class),
                mock(ListItemHydrator.class));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Adding LOVED removes the REJECTED membership for the same title.
     */
    @Test
    void addLovedRemovesRejectedSameTitle() {
        int userId = newUser("loved-removes-rejected");
        seed(userId, 42, TmdbMediaType.MOVIE, ListType.REJECTED);
        when(movieService.existsMovie(42)).thenReturn(true);
        authAs(userId);

        userListService.addToList(42, TmdbMediaType.MOVIE, ListType.LOVED);

        entityManager.clear();
        assertThat(count(userId, 42, TmdbMediaType.MOVIE, ListType.REJECTED)).isZero();
        assertThat(count(userId, 42, TmdbMediaType.MOVIE, ListType.LOVED)).isEqualTo(1);
    }

    /**
     * Adding the same title twice leaves one row and raises no error.
     */
    @Test
    void addTwiceLeavesOneRow() {
        int userId = newUser("add-twice");
        when(movieService.existsMovie(42)).thenReturn(true);
        authAs(userId);

        userListService.addToList(42, TmdbMediaType.MOVIE, ListType.LOVED);
        userListService.addToList(42, TmdbMediaType.MOVIE, ListType.LOVED);

        entityManager.clear();
        assertThat(count(userId, 42, TmdbMediaType.MOVIE, ListType.LOVED)).isEqualTo(1);
    }

    /**
     * Adding WATCHED does not delete other list memberships for the same title.
     */
    @Test
    void addWatchedKeepsOtherLists() {
        int userId = newUser("watched-keeps");
        seed(userId, 42, TmdbMediaType.MOVIE, ListType.WATCHLIST);
        seed(userId, 42, TmdbMediaType.MOVIE, ListType.LOVED);
        when(movieService.existsMovie(42)).thenReturn(true);
        authAs(userId);

        userListService.addToList(42, TmdbMediaType.MOVIE, ListType.WATCHED);

        entityManager.clear();
        assertThat(count(userId, 42, TmdbMediaType.MOVIE, ListType.WATCHLIST)).isEqualTo(1);
        assertThat(count(userId, 42, TmdbMediaType.MOVIE, ListType.LOVED)).isEqualTo(1);
        assertThat(count(userId, 42, TmdbMediaType.MOVIE, ListType.WATCHED)).isEqualTo(1);
    }

    /**
     * The conflict delete touches only the acting user's rows.
     */
    @Test
    void anotherUsersRowsStayUntouched() {
        int actingUser = newUser("acting");
        int otherUser = newUser("other");
        seed(otherUser, 42, TmdbMediaType.MOVIE, ListType.REJECTED);
        when(movieService.existsMovie(42)).thenReturn(true);
        authAs(actingUser);

        userListService.addToList(42, TmdbMediaType.MOVIE, ListType.LOVED);

        entityManager.clear();
        assertThat(count(otherUser, 42, TmdbMediaType.MOVIE, ListType.REJECTED)).isEqualTo(1);
    }

    /**
     * The insert sets created_at.
     */
    @Test
    void createdAtIsSet() {
        int userId = newUser("created-at");
        when(movieService.existsMovie(42)).thenReturn(true);
        authAs(userId);

        userListService.addToList(42, TmdbMediaType.MOVIE, ListType.LOVED);

        entityManager.clear();
        Object createdAt = entityManager.createNativeQuery(
                "select created_at from user_list_entries "
                        + "where user_id = ?1 and tmdb_id = ?2 and list_type = 'LOVED'")
                .setParameter(1, userId)
                .setParameter(2, 42)
                .getSingleResult();
        assertThat(createdAt).isNotNull();
    }

    /**
     * A missing title takes the TMDB slow path, then the row is inserted.
     */
    @Test
    void unknownTitleTakesSlowPathThenInserts() {
        int userId = newUser("slow-path");
        when(movieService.existsMovie(99)).thenReturn(false);
        Movie fetched = new Movie();
        fetched.setId(99);
        when(movieService.findOrFetchMovie(99)).thenReturn(fetched);
        authAs(userId);

        userListService.addToList(99, TmdbMediaType.MOVIE, ListType.LOVED);

        verify(movieService).findOrFetchMovie(99);
        entityManager.clear();
        assertThat(count(userId, 99, TmdbMediaType.MOVIE, ListType.LOVED)).isEqualTo(1);
    }

    /**
     * A missing title the slow path cannot fetch returns 404 and inserts nothing.
     */
    @Test
    void unknownTitleNotFoundInsertsNothing() {
        int userId = newUser("not-found");
        when(movieService.existsMovie(99)).thenReturn(false);
        when(movieService.findOrFetchMovie(99)).thenReturn(null);
        authAs(userId);

        ResponseEntity<?> response = userListService.addToList(99, TmdbMediaType.MOVIE, ListType.LOVED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        entityManager.clear();
        assertThat(count(userId, 99, TmdbMediaType.MOVIE, ListType.LOVED)).isZero();
    }

    private void authAs(int userId) {
        UserDetailsImpl principal = new UserDetailsImpl(userId, "u" + userId, "pw", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private int newUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password-long-enough");
        return userRepository.saveAndFlush(user).getId();
    }

    private void seed(int userId, int tmdbId, TmdbMediaType mediaType, ListType listType) {
        UserListEntry entry = new UserListEntry();
        entry.setUserId(userId);
        entry.setTmdbId(tmdbId);
        entry.setMediaType(mediaType);
        entry.setListType(listType);
        userListEntryRepository.saveAndFlush(entry);
    }

    private long count(int userId, int tmdbId, TmdbMediaType mediaType, ListType listType) {
        Number result = (Number) entityManager.createNativeQuery(
                "select count(*) from user_list_entries "
                        + "where user_id = ?1 and tmdb_id = ?2 and media_type = ?3 and list_type = ?4")
                .setParameter(1, userId)
                .setParameter(2, tmdbId)
                .setParameter(3, mediaType.name())
                .setParameter(4, listType.name())
                .getSingleResult();
        return result.longValue();
    }
}
