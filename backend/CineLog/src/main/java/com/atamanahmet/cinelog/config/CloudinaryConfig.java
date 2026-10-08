package com.atamanahmet.cinelog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    /**
     * Build the Cloudinary client when CLOUDINARY_URL is set.
     */
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${cloudinary.url:}')")
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        Cloudinary cloudinary = new Cloudinary(properties.url());
        cloudinary.config.timeout = properties.timeoutSeconds();
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
