package com.atamanahmet.cinelog.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atamanahmet.cinelog.domain.entity.CastMember;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.DiscoverRequest;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbGenre;
import com.atamanahmet.cinelog.service.GenreCacheService;
import com.atamanahmet.cinelog.service.TvShowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
class TvController {

        private final TvShowService tvShowService;
        private final GenreCacheService genreCacheService;

        @GetMapping("/tv/discover")
        public ResponseEntity<?> getTv(@Valid @ModelAttribute DiscoverRequest request) {
                return ResponseEntity.ok(tvShowService.discoverTvShows(request));
        }

        @GetMapping("/tv/genres")
        public ResponseEntity<List<TmdbGenre>> getTvGenres() {
                return ResponseEntity.ok(genreCacheService.getGenres(TmdbMediaType.TV));
        }

        /**
         * Search TV shows by free text. Optional minVotes drops low-vote TMDB hits.
         */
        @GetMapping("/tv/search")
        public ResponseEntity<?> searchHandler(
                        @RequestParam("query") @NotBlank @Size(max = 100) String query,
                        @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
                        @RequestParam(value = "minVotes", required = false) @Min(0) @Max(DiscoverRequest.MAX_VOTE_COUNT) Integer minVotes) {
                return ResponseEntity.ok(tvShowService.searchTvShows(query, page, minVotes));
        }

        @GetMapping("/tv/{mediaId}/video")
        public ResponseEntity<String> getVideo(@PathVariable(name = "mediaId", required = true) Integer mediaId) {
                Optional<String> trailer = tvShowService.getTrailer(mediaId);
                return trailer.map(url -> new ResponseEntity<>(url, HttpStatus.OK))
                                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }

        @GetMapping("/tv/{mediaId}/credits")
        public ResponseEntity<List<CastMember>> getCredits(@PathVariable(name = "mediaId") Integer mediaId) {
                return ResponseEntity.ok(tvShowService.getTopCast(mediaId));
        }

        @GetMapping("/tv/{mediaId}")
        public ResponseEntity<TvShowDto> getTvById(@PathVariable(name = "mediaId") Integer mediaId) {
                return ResponseEntity.ok(tvShowService.getDetailsFromTmdb(mediaId));
        }
}
