package com.atamanahmet.cinelog.service;

import java.util.Map;

import com.atamanahmet.cinelog.exception.ProfilePhotoStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Signed Cloudinary uploads for profile pictures. Same public id per user so replace has no orphan.
 */
@RequiredArgsConstructor
@Slf4j
public class CloudinaryProfilePhotoStorage implements ProfilePhotoStorage {

    private final Cloudinary cloudinary;

    /**
     * Upload image bytes under profile-pictures/user-{id} and return secure_url.
     */
    @Override
    public String upload(Integer userId, byte[] bytes, String contentType) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", publicId(userId),
                    "overwrite", true,
                    "invalidate", true,
                    "allowed_formats", "jpg,png"));
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new ProfilePhotoStorageException("Cloudinary upload returned no secure_url");
            }
            return secureUrl.toString();
        } catch (ProfilePhotoStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Cloudinary upload failed for userId={}", userId, ex);
            throw new ProfilePhotoStorageException("Profile photo upload failed", ex);
        }
    }

    /**
     * Destroy the Cloudinary asset for this user.
     */
    @Override
    public void delete(Integer userId) {
        try {
            cloudinary.uploader().destroy(publicId(userId), ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true));
        } catch (Exception ex) {
            log.error("Cloudinary delete failed for userId={}", userId, ex);
            throw new ProfilePhotoStorageException("Profile photo delete failed", ex);
        }
    }

    private static String publicId(Integer userId) {
        return "profile-pictures/user-" + userId;
    }
}
