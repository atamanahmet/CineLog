package com.atamanahmet.cinelog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atamanahmet.cinelog.domain.entity.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer> {

}
