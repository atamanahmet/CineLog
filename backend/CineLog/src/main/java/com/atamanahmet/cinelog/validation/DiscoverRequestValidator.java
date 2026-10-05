package com.atamanahmet.cinelog.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.atamanahmet.cinelog.dto.DiscoverRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Checks voteCount, year, rating, runtime, and genre include/exclude lists.
 */
public class DiscoverRequestValidator implements ConstraintValidator<ValidDiscoverRequest, DiscoverRequest> {

    @Override
    public boolean isValid(DiscoverRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        Integer voteCount = request.voteCount();
        if (voteCount == null
                || voteCount < 0
                || voteCount > DiscoverRequest.MAX_VOTE_COUNT) {
            add(context, "voteCount", "voteCount must be between 0 and 10000");
            valid = false;
        }
        int[] years = request.yearRange();
        if (years == null || years.length != 2) {
            add(context, "yearRange", "yearRange must have two values");
            valid = false;
        } else {
            if (years[0] > years[1]) {
                add(context, "yearRange", "yearRange minimum must be before maximum");
                valid = false;
            }
            if (years[0] < DiscoverRequest.MIN_YEAR || years[1] > DiscoverRequest.MAX_YEAR) {
                add(context, "yearRange", "yearRange must be between 1800 and 2100");
                valid = false;
            }
        }
        int[] ratings = request.ratingRange();
        if (ratings == null || ratings.length != 2) {
            add(context, "ratingRange", "ratingRange must have two values");
            valid = false;
        } else {
            if (ratings[0] > ratings[1]) {
                add(context, "ratingRange", "ratingRange minimum must be before maximum");
                valid = false;
            }
            if (ratings[0] < DiscoverRequest.MIN_RATING || ratings[1] > DiscoverRequest.MAX_RATING) {
                add(context, "ratingRange", "ratingRange must be between 0 and 10");
                valid = false;
            }
        }
        Integer minRuntime = request.minRuntime();
        if (minRuntime != null
                && (minRuntime < DiscoverRequest.MIN_RUNTIME || minRuntime > DiscoverRequest.MAX_RUNTIME)) {
            add(context, "minRuntime", "minRuntime must be between 0 and 400");
            valid = false;
        }
        Integer maxRuntime = request.maxRuntime();
        if (maxRuntime != null
                && (maxRuntime < DiscoverRequest.MIN_RUNTIME || maxRuntime > DiscoverRequest.MAX_RUNTIME)) {
            add(context, "maxRuntime", "maxRuntime must be between 0 and 400");
            valid = false;
        }
        if (minRuntime != null && maxRuntime != null && minRuntime > maxRuntime) {
            add(context, "minRuntime", "minRuntime must be before maxRuntime");
            valid = false;
        }
        if (!validateIdList(context, "genreIdList", request.genreIdList())) {
            valid = false;
        }
        if (!validateIdList(context, "withoutGenres", request.withoutGenres())) {
            valid = false;
        }
        if (!validateNoOverlap(context, request.genreIdList(), request.withoutGenres())) {
            valid = false;
        }
        return valid;
    }

    /**
     * Positive ints, max GenreFilterValidator.MAX_ITEMS, no duplicates.
     */
    private static boolean validateIdList(
            ConstraintValidatorContext context, String field, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        boolean valid = true;
        if (ids.size() > GenreFilterValidator.MAX_ITEMS) {
            add(context, field, field + " length out of range");
            valid = false;
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer id : ids) {
            if (id == null || id < 1) {
                add(context, field, field + " items must be positive ints");
                valid = false;
                break;
            }
            if (!seen.add(id)) {
                add(context, field, field + " must not contain duplicates");
                valid = false;
                break;
            }
        }
        return valid;
    }

    /**
     * Reject ids present in both include and exclude lists.
     */
    private static boolean validateNoOverlap(
            ConstraintValidatorContext context,
            List<Integer> include,
            List<Integer> exclude) {
        if (include == null || include.isEmpty() || exclude == null || exclude.isEmpty()) {
            return true;
        }
        Set<Integer> includeSet = new HashSet<>(include);
        for (Integer id : exclude) {
            if (id != null && includeSet.contains(id)) {
                add(context, "withoutGenres", "genreIdList and withoutGenres must not share an id");
                return false;
            }
        }
        return true;
    }

    private static void add(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
