package com.atamanahmet.cinelog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atamanahmet.cinelog.config.ContentPolicyProperties;
import com.atamanahmet.cinelog.dto.SiteConfigResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SiteConfigController {

    private final ContentPolicyProperties contentPolicy;

    @GetMapping
    public ResponseEntity<SiteConfigResponse> getConfig() {
        return ResponseEntity.ok(new SiteConfigResponse(contentPolicy.adultEnabled()));
    }
}
