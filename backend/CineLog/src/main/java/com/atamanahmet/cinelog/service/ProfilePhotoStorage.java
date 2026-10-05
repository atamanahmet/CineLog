package com.atamanahmet.cinelog.service;

/**
 * Profile photo upload and delete against an external image store.
 */
public interface ProfilePhotoStorage {

    /**
     * Upload image bytes for one user and return the secure URL.
     */
    String upload(Integer userId, byte[] bytes, String contentType);

    /**
     * Delete the stored image for one user when present.
     */
    void delete(Integer userId);
}
