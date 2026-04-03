package com.atamanahmet.cinelog.domain.POJO;

import java.util.ArrayList;

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
public class Cast {

    private Integer id;

    private ArrayList<CastMember> cast;

    private ArrayList<CastMember> crew;

}