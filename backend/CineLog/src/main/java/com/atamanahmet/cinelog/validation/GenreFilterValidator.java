package com.atamanahmet.cinelog.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Genre include/exclude list rules for recommendation endpoints.
 */
public final class GenreFilterValidator {

    public static final int MAX_ITEMS = 25;

    private GenreFilterValidator() {
    }

    /**
     * Null becomes empty. Otherwise an unmodifiable copy. Call after validate.
     */
    public static List<Integer> normalize(List<Integer> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return List.copyOf(raw);
    }

    /**
     * Positive ints, max 25, no duplicates per list, no shared id across lists.
     */
    public static void validate(List<Integer> genreInclude, List<Integer> genreExclude) {
        List<Integer> include = genreInclude == null ? List.of() : genreInclude;
        List<Integer> exclude = genreExclude == null ? List.of() : genreExclude;
        validateOne("genreInclude", include);
        validateOne("genreExclude", exclude);
        Set<Integer> shared = new HashSet<>();
        for (Integer id : include) {
            shared.add(id);
        }
        for (Integer id : exclude) {
            if (shared.contains(id)) {
                throw badRequest("genreInclude and genreExclude must not share an id");
            }
        }
    }

    /**
     * True when either list has at least one id.
     */
    public static boolean isFiltered(List<Integer> genreInclude, List<Integer> genreExclude) {
        return (genreInclude != null && !genreInclude.isEmpty())
                || (genreExclude != null && !genreExclude.isEmpty());
    }

    private static void validateOne(String field, List<Integer> ids) {
        if (ids.size() > MAX_ITEMS) {
            throw badRequest(field + " length out of range");
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer id : ids) {
            if (id == null || id < 1) {
                throw badRequest(field + " items must be positive ints");
            }
            if (!seen.add(id)) {
                throw badRequest(field + " must not contain duplicates");
            }
        }
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
