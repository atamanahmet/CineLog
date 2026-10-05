package com.atamanahmet.cinelog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atamanahmet.cinelog.service.CloudinaryProfilePhotoStorage;
import com.atamanahmet.cinelog.service.DisabledProfilePhotoStorage;
import com.atamanahmet.cinelog.service.ProfilePhotoStorage;
import com.cloudinary.Cloudinary;

/**
 * Optional Cloudinary client and profile photo storage. Blank URL uses disabled storage.
 */
@Configuration
public class CloudinaryConfig {

    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Build the Cloudinary client when CLOUDINARY_URL is set.
     */
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${cloudinary.url:}')")
    public Cloudinary cloudinary(@Value("${cloudinary.url}") String url) {
        Cloudinary cloudinary = new Cloudinary(url);
        cloudinary.config.timeout = TIMEOUT_SECONDS;
        return cloudinary;
    }

    /**
     * Real Cloudinary storage when the client bean exists.
     */
    @Bean
    @ConditionalOnBean(Cloudinary.class)
    public ProfilePhotoStorage cloudinaryProfilePhotoStorage(Cloudinary cloudinary) {
        return new CloudinaryProfilePhotoStorage(cloudinary);
    }

    /**
     * Disabled stand-in when Cloudinary is not configured.
     */
    @Bean
    @ConditionalOnMissingBean(ProfilePhotoStorage.class)
    public ProfilePhotoStorage disabledProfilePhotoStorage() {
        return new DisabledProfilePhotoStorage();
    }
}
