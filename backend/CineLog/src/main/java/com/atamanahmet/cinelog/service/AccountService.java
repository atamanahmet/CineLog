package com.atamanahmet.cinelog.service;

import com.atamanahmet.cinelog.dto.AccountResponse;
import com.atamanahmet.cinelog.dto.ChangeEmailRequest;
import com.atamanahmet.cinelog.dto.ChangePasswordRequest;
import com.atamanahmet.cinelog.dto.RecommendationSettingsDto;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Account settings for the logged-in user.
 */
public interface AccountService {

    AccountResponse getAccount();

    AccountResponse changeEmail(ChangeEmailRequest request);

    /**
     * Update the password, revoke every other session, and reissue cookies for this one.
     */
    void changePassword(ChangePasswordRequest request, HttpServletResponse response);

    RecommendationSettingsDto getRecommendationSettings();

    RecommendationSettingsDto updateRecommendationSettings(RecommendationSettingsDto settings);
}
