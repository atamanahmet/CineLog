package com.atamanahmet.cinelog.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.domain.POJO.Movie;
import com.atamanahmet.cinelog.repository.MovieRepository;

@Service
public class MovieService {

    @Autowired
    MovieRepository movieRepository;

    public void saveMovie(Movie movie) {
        movieRepository.save(movie);
    }

    public Movie findMovieById(Integer id) {
        return movieRepository.findById(id).orElse(null);
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public List<Movie> getMoviesFromIdSet(Set<Integer> idSet) {
        return movieRepository.findAllById(idSet);
    }
}
