package com.atamanahmet.cinelog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atamanahmet.cinelog.domain.POJO.TvShow;

@Repository
public interface TvShowRepository extends JpaRepository<TvShow, Integer> {

}
