package com.atamanahmet.cinelog.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.dto.DiscoverRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DiscoverRequestValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void emptyGenreListsValid() {
        DiscoverRequest request = base().withDefaults(
                com.atamanahmet.cinelog.domain.entity.TmdbMediaType.MOVIE);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void overlapRejected() {
        DiscoverRequest request = new DiscoverRequest(
                false, "", 100, null, 1, List.of(28), null, null, "en", null, null, List.of(28));
        Set<ConstraintViolation<DiscoverRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("must not share an id"));
    }

    @Test
    void duplicatesInWithoutGenresRejected() {
        DiscoverRequest request = new DiscoverRequest(
                false, "", 100, null, 1, List.of(), null, null, "en", null, null, List.of(16, 16));
        Set<ConstraintViolation<DiscoverRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("duplicates"));
    }

    @Test
    void nonPositiveWithoutGenresRejected() {
        DiscoverRequest request = new DiscoverRequest(
                false, "", 100, null, 1, List.of(), null, null, "en", null, null, List.of(0));
        Set<ConstraintViolation<DiscoverRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("positive ints"));
    }

    @Test
    void tooManyWithoutGenresRejected() {
        List<Integer> tooMany = IntStream.rangeClosed(1, GenreFilterValidator.MAX_ITEMS + 1)
                .boxed()
                .toList();
        DiscoverRequest request = new DiscoverRequest(
                false, "", 100, null, 1, List.of(), null, null, "en", null, null, tooMany);
        Set<ConstraintViolation<DiscoverRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("length out of range"));
    }

    @Test
    void includeAndExcludeTogetherValid() {
        DiscoverRequest request = new DiscoverRequest(
                false, "", 100, null, 1, List.of(878), null, null, "en", null, null, List.of(16));
        assertThat(validator.validate(request)).isEmpty();
    }

    private static DiscoverRequest base() {
        return new DiscoverRequest(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
