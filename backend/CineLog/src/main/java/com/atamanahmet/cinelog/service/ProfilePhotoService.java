package com.atamanahmet.cinelog.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.ProfilePhotoResponse;
import com.atamanahmet.cinelog.exception.ProfilePhotoStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate and store profile photos. Not transactional; DB writes go through UserService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePhotoService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");

    private final ProfilePhotoStorage profilePhotoStorage;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    /**
     * Validate, upload to storage, then persist the secure URL.
     */
    public ResponseEntity<?> uploadPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is null");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body("Unsupported file type");
        }
        User user = currentUserService.getCurrentUser();
        Integer userId = user.getId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
        }
        String url = profilePhotoStorage.upload(userId, bytes, contentType);
        try {
            userService.updateProfilePhotoUrl(userId, url);
        } catch (RuntimeException ex) {
            try {
                profilePhotoStorage.delete(userId);
            } catch (Exception deleteEx) {
                log.error("Best-effort profile photo delete after DB failure failed, userId={}", userId, deleteEx);
            }
            throw ex;
        }
        return ResponseEntity.ok(new ProfilePhotoResponse(url));
    }

    /**
     * Clear the stored URL first, then best-effort delete from storage.
     */
    public ResponseEntity<Void> deletePhoto() {
        User user = currentUserService.getCurrentUser();
        Integer userId = user.getId();
        userService.updateProfilePhotoUrl(userId, null);
        try {
            profilePhotoStorage.delete(userId);
        } catch (ProfilePhotoStorageException ex) {
            if (!ex.isStorageDisabled()) {
                log.error("Profile photo storage delete failed after DB clear, userId={}", userId, ex);
            }
        }
        return ResponseEntity.noContent().build();
    }
}
