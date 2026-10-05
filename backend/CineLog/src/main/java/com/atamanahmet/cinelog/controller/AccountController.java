package com.atamanahmet.cinelog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atamanahmet.cinelog.dto.AccountResponse;
import com.atamanahmet.cinelog.dto.ChangeEmailRequest;
import com.atamanahmet.cinelog.dto.ChangePasswordRequest;
import com.atamanahmet.cinelog.dto.RecommendationSettingsDto;
import com.atamanahmet.cinelog.service.AccountService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<AccountResponse> getAccount() {
        return ResponseEntity.ok(accountService.getAccount());
    }

    @PutMapping("/email")
    public ResponseEntity<AccountResponse> changeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        return ResponseEntity.ok(accountService.changeEmail(request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {
        accountService.changePassword(request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recommendation-settings")
    public ResponseEntity<RecommendationSettingsDto> getRecommendationSettings() {
        return ResponseEntity.ok(accountService.getRecommendationSettings());
    }

    @PutMapping("/recommendation-settings")
    public ResponseEntity<RecommendationSettingsDto> updateRecommendationSettings(
            @Valid @RequestBody RecommendationSettingsDto settings) {
        return ResponseEntity.ok(accountService.updateRecommendationSettings(settings));
    }
}
