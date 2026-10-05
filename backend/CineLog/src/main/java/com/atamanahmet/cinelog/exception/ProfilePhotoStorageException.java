package com.atamanahmet.cinelog.exception;

/**
 * Profile photo storage failure. Client sees a generic 503, not the provider message.
 */
public class ProfilePhotoStorageException extends RuntimeException {

    private final boolean storageDisabled;

    public ProfilePhotoStorageException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public ProfilePhotoStorageException(String message) {
        this(message, null, false);
    }

    private ProfilePhotoStorageException(String message, Throwable cause, boolean storageDisabled) {
        super(message, cause);
        this.storageDisabled = storageDisabled;
    }

    /**
     * Storage bean is the disabled stand-in. Not a provider outage.
     */
    public static ProfilePhotoStorageException disabled() {
        return new ProfilePhotoStorageException("Profile photo storage is unavailable", null, true);
    }

    public boolean isStorageDisabled() {
        return storageDisabled;
    }
}
