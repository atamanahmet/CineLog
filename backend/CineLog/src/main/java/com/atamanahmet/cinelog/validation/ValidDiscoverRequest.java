package com.atamanahmet.cinelog.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Year and rating range order and bounds for discover queries.
 */
@Documented
@Constraint(validatedBy = DiscoverRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDiscoverRequest {

    String message() default "Invalid discover filters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
