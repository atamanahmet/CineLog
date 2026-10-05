package com.atamanahmet.cinelog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atamanahmet.cinelog.dto.DiscoverDefaultsResponse;
import com.atamanahmet.cinelog.mapper.DiscoverDefaultsMapper;

/**
 * Public discover metadata endpoints.
 */
@RestController
@RequestMapping("/api/discover")
class DiscoverController {

    /**
     * Return discover filter defaults and bounds.
     */
    @GetMapping("/defaults")
    public ResponseEntity<DiscoverDefaultsResponse> getDefaults() {
        return ResponseEntity.ok(DiscoverDefaultsMapper.fromConstants());
    }
}
