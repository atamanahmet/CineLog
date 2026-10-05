package com.atamanahmet.cinelog.service;

import java.util.List;

import com.atamanahmet.cinelog.domain.entity.CastMember;

public record DetailsBundle<T>(T details, List<CastMember> topCast, String trailerUrl) {
    public DetailsBundle {
        topCast = topCast == null ? List.of() : List.copyOf(topCast);
    }
}
