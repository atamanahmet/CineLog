package com.atamanahmet.cinelog.dto.tmdb;

import java.util.List;

import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCast {

    private Integer id;

    private List<CastMember> cast;

    private List<CastMember> crew;

}
