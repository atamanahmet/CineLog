package com.atamanahmet.cinelog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atamanahmet.cinelog.dto.PersonDetailDto;
import com.atamanahmet.cinelog.service.PersonService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class PersonController {

    private final PersonService personService;

    /**
     * Return person details and credits by TMDB id.
     */
    @GetMapping("/person/{id}")
    public ResponseEntity<PersonDetailDto> getPersonById(@PathVariable(name = "id") Integer id) {
        return ResponseEntity.ok(personService.getPersonById(id));
    }
}
