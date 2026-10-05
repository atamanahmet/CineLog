package com.atamanahmet.cinelog.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import com.atamanahmet.cinelog.service.CloudinaryProfilePhotoStorage;
import com.atamanahmet.cinelog.service.DisabledProfilePhotoStorage;
import com.atamanahmet.cinelog.service.ProfilePhotoStorage;
import com.cloudinary.Cloudinary;

/**
 * Storage bean selection from cloudinary.url. No network calls.
 */
class CloudinaryConfigTest {

    @Nested
    @SpringBootTest(classes = CloudinaryConfig.class)
    @TestPropertySource(properties = "cloudinary.url=")
    class WhenUrlBlank {

        @Autowired
        private ProfilePhotoStorage profilePhotoStorage;

        @Autowired
        private ApplicationContext applicationContext;

        /**
         * Blank URL wires the disabled stand-in and skips the Cloudinary client.
         */
        @Test
        void usesDisabledStorage() {
            assertInstanceOf(DisabledProfilePhotoStorage.class, profilePhotoStorage);
            assertEquals(0, applicationContext.getBeanNamesForType(Cloudinary.class).length);
        }
    }

    @Nested
    @SpringBootTest(classes = CloudinaryConfig.class)
    @TestPropertySource(properties = "cloudinary.url=cloudinary://api_key:api_secret@demo")
    class WhenUrlSet {

        @Autowired
        private ProfilePhotoStorage profilePhotoStorage;

        @Autowired
        private ApplicationContext applicationContext;

        /**
         * Set URL wires real Cloudinary storage. Client constructed locally only.
         */
        @Test
        void usesCloudinaryStorage() {
            assertInstanceOf(CloudinaryProfilePhotoStorage.class, profilePhotoStorage);
            assertEquals(1, applicationContext.getBeanNamesForType(Cloudinary.class).length);
        }
    }
}
