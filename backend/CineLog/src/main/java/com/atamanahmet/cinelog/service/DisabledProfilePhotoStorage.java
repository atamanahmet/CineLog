package com.atamanahmet.cinelog.service;

import com.atamanahmet.cinelog.exception.ProfilePhotoStorageException;

/**
 * Used when CLOUDINARY_URL is missing. Upload and delete always fail.
 */
public class DisabledProfilePhotoStorage implements ProfilePhotoStorage {

    /**
     * Reject upload when Cloudinary is not configured.
     */
    @Override
    public String upload(Integer userId, byte[] bytes, String contentType) {
        throw ProfilePhotoStorageException.disabled();
    }

    /**
     * Reject delete when Cloudinary is not configured.
     */
    @Override
    public void delete(Integer userId) {
        throw ProfilePhotoStorageException.disabled();
    }
}
