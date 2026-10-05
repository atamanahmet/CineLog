package com.atamanahmet.cinelog.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;

import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    ResponseEntity<?> getUserArchive(HttpServletRequest request);

    User loadByUserName(String username);

    /**
     * Loved movie and TV keys for one user.
     */
    Set<MediaKey> findLovedKeys(Integer userId);

    /**
     * Replace stored recommendation keys for one media type inside a short transaction.
     */
    void replaceRecommendation(Integer userId, TmdbMediaType mediaType, List<MediaKey> keys);

    /**
     * Persist the profile photo URL for one user inside a short transaction.
     */
    void updateProfilePhotoUrl(Integer userId, String url);
}
