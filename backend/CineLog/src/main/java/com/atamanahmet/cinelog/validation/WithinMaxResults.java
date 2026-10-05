package com.atamanahmet.cinelog.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Rejects maxResults above the configured recommendation.limits.max-results cap.
 */
@Documented
@Constraint(validatedBy = WithinMaxResultsValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface WithinMaxResults {

    String message() default "must be at most the configured recommendation results limit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
