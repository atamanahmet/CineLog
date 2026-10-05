package com.atamanahmet.cinelog.validation;

import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.config.RecommendationLimitsProperties;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Compares maxResults against the runtime RecommendationLimitsProperties cap.
 */
@Component
public class WithinMaxResultsValidator implements ConstraintValidator<WithinMaxResults, Integer> {

    private final RecommendationLimitsProperties limitsProperties;

    public WithinMaxResultsValidator(RecommendationLimitsProperties limitsProperties) {
        this.limitsProperties = limitsProperties;
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value <= limitsProperties.getMaxResults();
    }
}
