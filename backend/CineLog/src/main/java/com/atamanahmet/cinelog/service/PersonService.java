package com.atamanahmet.cinelog.service;

import com.atamanahmet.cinelog.dto.PersonDetailDto;

public interface PersonService {

    /**
     * Return person details and credits from cache, or fetch once from TMDB.
     */
    PersonDetailDto getPersonById(Integer id);
}
