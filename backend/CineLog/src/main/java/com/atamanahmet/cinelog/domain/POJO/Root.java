package com.atamanahmet.cinelog.domain.POJO;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Root {
    public int page;
    public ArrayList<Movie> results;
    public int total_pages;
    public int total_results;
}
