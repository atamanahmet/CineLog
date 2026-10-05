package com.atamanahmet.cinelog.dto;

import java.util.List;

/**
 * One merged movie or TV credit for a person.
 */
public record PersonCreditDto(
        String mediaType,
        Integer tmdbId,
        String title,
        String posterPath,
        String date,
        Double voteAverage,
        List<Integer> genreIds,
        List<PersonCreditRoleDto> roles) {

    /**
     * Treat null lists as empty.
     */
    public PersonCreditDto {
        genreIds = genreIds == null ? List.of() : genreIds;
        roles = roles == null ? List.of() : roles;
    }
}
