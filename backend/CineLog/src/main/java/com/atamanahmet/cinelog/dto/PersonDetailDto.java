package com.atamanahmet.cinelog.dto;

import java.util.List;

/**
 * Person detail payload returned by the API and stored in cache.
 */
public record PersonDetailDto(
        PersonInfoDto person,
        List<PersonCreditDto> credits) {

    /**
     * Treat a null credits list as empty.
     */
    public PersonDetailDto {
        credits = credits == null ? List.of() : credits;
    }
}
