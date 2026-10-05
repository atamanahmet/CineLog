package com.atamanahmet.cinelog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.ProfilePhotoResponse;
import com.atamanahmet.cinelog.exception.ApiExceptionHandler;
import com.atamanahmet.cinelog.exception.ProfilePhotoStorageException;
import com.atamanahmet.cinelog.mapper.UserMapper;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.impl.UserServiceImpl;

import jakarta.persistence.EntityManager;

@DataJpaTest
@EnableConfigurationProperties(RecommendationLimitsProperties.class)
@Import({
        UserServiceImpl.class,
        ListItemHydrator.class,
        RecommendationExclusionService.class,
        ProfilePhotoService.class,
        CacheConfig.class,
        ProfilePhotoServiceTest.FakeStorage.class
})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "recommendation.limits.max-exclude=10000"
})
class ProfilePhotoServiceTest {

    @Autowired
    private ProfilePhotoService profilePhotoService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FakeStorage fakeStorage;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private TvShowService tvShowService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private RecommendationHydrator recommendationHydrator;

    private User user;

    @BeforeEach
    void setUp() {
        fakeStorage.reset();
        user = new User("photo-user", "password-hash-long-enough");
        user = userRepository.saveAndFlush(user);
        when(currentUserService.getCurrentUser()).thenAnswer(invocation ->
                userRepository.findById(user.getId()).orElseThrow());
    }

    /**
     * Successful upload stores the secure URL on the user after reload.
     */
    @Test
    void uploadSuccessStoresUrl() {
        MockMultipartFile file = jpeg("hello");
        ResponseEntity<?> response = profilePhotoService.uploadPhoto(file);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ProfilePhotoResponse body = (ProfilePhotoResponse) response.getBody();
        assertEquals(fakeStorage.urlFor(user.getId()), body.profilePictureUrl());

        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(fakeStorage.urlFor(user.getId()), reloaded.getProfilePictureUrl());
    }

    /**
     * Storage failure leaves the DB URL unchanged.
     */
    @Test
    void storageFailureLeavesDbUnchanged() {
        fakeStorage.failUpload = true;
        assertThrows(ProfilePhotoStorageException.class,
                () -> profilePhotoService.uploadPhoto(jpeg("x")));
        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertNull(reloaded.getProfilePictureUrl());
        assertEquals(0, fakeStorage.deleteCalls.get());
    }

    /**
     * Disabled storage upload maps to 503 and leaves the DB URL unchanged.
     */
    @Test
    void disabledStorageUploadReturns503PathAndLeavesDbUnchanged() {
        user.setProfilePictureUrl("https://example.com/existing.jpg");
        userRepository.saveAndFlush(user);

        ProfilePhotoService service = new ProfilePhotoService(
                new DisabledProfilePhotoStorage(), userService, currentUserService);
        ProfilePhotoStorageException thrown = assertThrows(ProfilePhotoStorageException.class,
                () -> service.uploadPhoto(jpeg("x")));
        assertTrue(thrown.isStorageDisabled());

        ResponseEntity<ProblemDetail> problem = new ApiExceptionHandler().handleProfilePhotoStorage(thrown);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, problem.getStatusCode());
        assertEquals("Profile photo storage is unavailable", problem.getBody().getDetail());

        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("https://example.com/existing.jpg", reloaded.getProfilePictureUrl());
    }

    /**
     * Disabled storage delete still clears the DB URL.
     */
    @Test
    void disabledStorageDeleteStillClearsDb() {
        user.setProfilePictureUrl("https://example.com/existing.jpg");
        userRepository.saveAndFlush(user);

        ProfilePhotoService service = new ProfilePhotoService(
                new DisabledProfilePhotoStorage(), userService, currentUserService);
        ResponseEntity<Void> response = service.deletePhoto();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertNull(reloaded.getProfilePictureUrl());
    }

    /**
     * DB write failure triggers best-effort storage delete and rethrows.
     */
    @Test
    void dbWriteFailureDeletesStorageAndRethrows() {
        UserService failingUserService = new UserService() {
            @Override
            public void updateProfilePhotoUrl(Integer userId, String url) {
                throw new IllegalStateException("db down");
            }

            @Override
            public ResponseEntity<?> getUserArchive(jakarta.servlet.http.HttpServletRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public User loadByUserName(String username) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Set<com.atamanahmet.cinelog.domain.entity.MediaKey> findLovedKeys(Integer userId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void replaceRecommendation(Integer userId,
                    com.atamanahmet.cinelog.domain.entity.TmdbMediaType mediaType,
                    java.util.List<com.atamanahmet.cinelog.domain.entity.MediaKey> keys) {
                throw new UnsupportedOperationException();
            }
        };
        ProfilePhotoService service = new ProfilePhotoService(fakeStorage, failingUserService, currentUserService);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.uploadPhoto(jpeg("y")));
        assertEquals("db down", thrown.getMessage());
        assertEquals(1, fakeStorage.deleteCalls.get());
        assertTrue(fakeStorage.bytesByUser.isEmpty());
    }

    /**
     * Replace overwrites the same user slot with no leftover entry.
     */
    @Test
    void replaceOverwritesWithoutLeftovers() {
        profilePhotoService.uploadPhoto(jpeg("first"));
        profilePhotoService.uploadPhoto(jpeg("second"));
        assertEquals(1, fakeStorage.bytesByUser.size());
        assertEquals("second", new String(fakeStorage.bytesByUser.get(user.getId()), StandardCharsets.UTF_8));
        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(fakeStorage.urlFor(user.getId()), reloaded.getProfilePictureUrl());
    }

    /**
     * Delete clears the DB URL then calls storage delete.
     */
    @Test
    void deleteClearsDbThenStorage() {
        profilePhotoService.uploadPhoto(jpeg("keep"));
        ResponseEntity<Void> response = profilePhotoService.deletePhoto();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(1, fakeStorage.deleteCalls.get());
        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertNull(reloaded.getProfilePictureUrl());
        assertTrue(fakeStorage.bytesByUser.isEmpty());
    }

    /**
     * Storage delete failure after DB clear does not fail the request.
     */
    @Test
    void storageDeleteFailureDoesNotFailRequest() {
        profilePhotoService.uploadPhoto(jpeg("keep"));
        fakeStorage.failDelete = true;
        ResponseEntity<Void> response = profilePhotoService.deletePhoto();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertNull(reloaded.getProfilePictureUrl());
    }

    /**
     * Invalid content type is rejected before storage.
     */
    @Test
    void invalidContentTypeRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture", "x.gif", "image/gif", "x".getBytes(StandardCharsets.UTF_8));
        ResponseEntity<?> response = profilePhotoService.uploadPhoto(file);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(fakeStorage.bytesByUser.isEmpty());
        entityManager.flush();
        entityManager.clear();
        assertNull(userRepository.findById(user.getId()).orElseThrow().getProfilePictureUrl());
    }

    private static MockMultipartFile jpeg(String content) {
        return new MockMultipartFile(
                "profilePicture", "pic.jpg", "image/jpeg", content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * In-memory ProfilePhotoStorage for tests. No network.
     */
    @org.springframework.stereotype.Component
    static class FakeStorage implements ProfilePhotoStorage {

        final Map<Integer, byte[]> bytesByUser = new ConcurrentHashMap<>();
        final AtomicInteger deleteCalls = new AtomicInteger();
        boolean failUpload;
        boolean failDelete;

        void reset() {
            bytesByUser.clear();
            deleteCalls.set(0);
            failUpload = false;
            failDelete = false;
        }

        String urlFor(Integer userId) {
            return "https://res.cloudinary.com/demo/image/upload/profile-pictures/user-" + userId;
        }

        @Override
        public String upload(Integer userId, byte[] bytes, String contentType) {
            if (failUpload) {
                throw new ProfilePhotoStorageException("upload failed");
            }
            bytesByUser.put(userId, bytes);
            return urlFor(userId);
        }

        @Override
        public void delete(Integer userId) {
            deleteCalls.incrementAndGet();
            if (failDelete) {
                throw new ProfilePhotoStorageException("delete failed");
            }
            bytesByUser.remove(userId);
        }
    }
}
