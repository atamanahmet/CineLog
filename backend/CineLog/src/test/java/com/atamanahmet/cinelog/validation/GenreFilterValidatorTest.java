package com.atamanahmet.cinelog.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.web.server.ResponseStatusException;

class GenreFilterValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    void emptyOrNullAccepted(List<Integer> empty) {
        GenreFilterValidator.validate(empty, empty);
        assertThat(GenreFilterValidator.isFiltered(empty, empty)).isFalse();
    }

    @Test
    void overlapRejected() {
        assertThatThrownBy(() -> GenreFilterValidator.validate(List.of(28), List.of(28)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must not share an id");
    }

    @Test
    void duplicatesRejected() {
        assertThatThrownBy(() -> GenreFilterValidator.validate(List.of(28, 28), List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    void tooManyRejected() {
        List<Integer> tooMany = IntStream.rangeClosed(1, GenreFilterValidator.MAX_ITEMS + 1)
                .boxed()
                .toList();
        assertThatThrownBy(() -> GenreFilterValidator.validate(tooMany, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("length out of range");
    }

    @Test
    void nonPositiveRejected() {
        assertThatThrownBy(() -> GenreFilterValidator.validate(List.of(0), List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("positive ints");
        assertThatThrownBy(() -> GenreFilterValidator.validate(List.of(-1), List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("positive ints");
        List<Integer> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> GenreFilterValidator.validate(withNull, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("positive ints");
    }

    @Test
    void validListsAccepted() {
        GenreFilterValidator.validate(List.of(878, 28), List.of(16));
        assertThat(GenreFilterValidator.isFiltered(List.of(878), List.of())).isTrue();
        assertThat(GenreFilterValidator.isFiltered(List.of(), List.of(16))).isTrue();
    }
}
