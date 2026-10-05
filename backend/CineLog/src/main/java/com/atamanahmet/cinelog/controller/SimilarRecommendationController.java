package com.atamanahmet.cinelog.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationLovedPayload;
import com.atamanahmet.cinelog.dto.SimilarRecommendationRequest;
import com.atamanahmet.cinelog.dto.SimilarSeedDto;
import com.atamanahmet.cinelog.service.RecommendationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class SimilarRecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Scored similar rows for one media type, seeded by the request body.
     */
    @PostMapping("/similar")
    public ResponseEntity<List<RecommendationItemDTO>> findSimilar(
            @Valid @RequestBody SimilarRecommendationRequest request) {
        List<RecommendationLovedPayload> seeds = request.seeds().stream()
                .map(SimilarRecommendationController::toLovedPayload)
                .toList();
        return ResponseEntity.ok(recommendationService.findSimilar(
                seeds, request.mediaType(), request.genreInclude(), request.genreExclude()));
    }

    private static RecommendationLovedPayload toLovedPayload(SimilarSeedDto seed) {
        return new RecommendationLovedPayload(seed.tmdbId(), seed.mediaType());
    }
}
