package com.atamanahmet.cinelog.dto;

import java.util.List;

/**
 * Nested person profile fields for the person details response.
 */
public record PersonInfoDto(
        Integer id,
        String name,
        String biography,
        String birthday,
        String deathday,
        String placeOfBirth,
        String profilePath,
        String knownForDepartment,
        List<String> alsoKnownAs) {

    /**
     * Treat a null alsoKnownAs list as empty.
     */
    public PersonInfoDto {
        alsoKnownAs = alsoKnownAs == null ? List.of() : alsoKnownAs;
    }
}
