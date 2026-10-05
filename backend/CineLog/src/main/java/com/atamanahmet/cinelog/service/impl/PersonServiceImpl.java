package com.atamanahmet.cinelog.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.client.tmdb.TmdbClient;
import com.atamanahmet.cinelog.config.CacheConfig;
import com.atamanahmet.cinelog.dto.PersonDetailDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPersonResponse;
import com.atamanahmet.cinelog.mapper.PersonMapper;
import com.atamanahmet.cinelog.service.PersonService;
import com.github.benmanes.caffeine.cache.Cache;

@Service
public class PersonServiceImpl implements PersonService {

    private final Cache<Integer, PersonDetailDto> personCache;
    private final TmdbClient tmdbClient;
    private final PersonMapper personMapper;

    public PersonServiceImpl(
            @Qualifier(CacheConfig.PERSON_CACHE) Cache<Integer, PersonDetailDto> personCache,
            TmdbClient tmdbClient,
            PersonMapper personMapper) {
        this.personCache = personCache;
        this.tmdbClient = tmdbClient;
        this.personMapper = personMapper;
    }

    /**
     * Return person details and credits from cache, or fetch once from TMDB.
     */
    @Override
    public PersonDetailDto getPersonById(Integer id) {
        return personCache.get(id, this::loadPerson);
    }

    private PersonDetailDto loadPerson(Integer id) {
        TmdbPersonResponse person = tmdbClient.getPersonById(id);
        return personMapper.toDetail(person);
    }
}
