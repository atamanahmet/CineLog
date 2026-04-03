package com.atamanahmet.cinelog.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
public class SearchEntry {
    public SearchEntry() {
    }

    private String query;

    private LocalDateTime date;

}
