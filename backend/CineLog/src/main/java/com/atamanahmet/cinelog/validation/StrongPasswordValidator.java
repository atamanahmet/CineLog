package com.atamanahmet.cinelog.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_CHARACTERS = 8;
    private static final Set<String> COMMON_PASSWORDS = loadCommonPasswords();

    /**
     * Loads the common-password blocklist from the classpath once.
     */
    private static Set<String> loadCommonPasswords() {
        InputStream stream = StrongPasswordValidator.class.getResourceAsStream("/common-passwords.txt");
        if (stream == null) {
            throw new IllegalStateException("Missing classpath resource: common-passwords.txt");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            Set<String> passwords = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    passwords.add(trimmed.toLowerCase(Locale.ROOT));
                }
            }
            return Set.copyOf(passwords);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load classpath resource: common-passwords.txt", e);
        }
    }

    /**
     * Checks length, required character classes, and common-password blocklist. Reports each broken rule.
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            addViolation(context, "This password is too common");
            valid = false;
        }

        int length = password.codePointCount(0, password.length());
        if (length < MIN_CHARACTERS) {
            addViolation(context, "Password must be at least 8 characters");
            valid = false;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            addViolation(context, "Password is too long");
            valid = false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        int index = 0;
        while (index < password.length()) {
            int codePoint = password.codePointAt(index);
            if (Character.isUpperCase(codePoint)) {
                hasUpper = true;
            } else if (Character.isLowerCase(codePoint)) {
                hasLower = true;
            } else if (Character.isDigit(codePoint)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(codePoint)) {
                hasSpecial = true;
            }
            index += Character.charCount(codePoint);
        }

        if (!hasUpper) {
            addViolation(context, "Password must contain an uppercase letter");
            valid = false;
        }
        if (!hasLower) {
            addViolation(context, "Password must contain a lowercase letter");
            valid = false;
        }
        if (!hasDigit) {
            addViolation(context, "Password must contain a number");
            valid = false;
        }
        if (!hasSpecial) {
            addViolation(context, "Password must contain a special character");
            valid = false;
        }

        return valid;
    }

    /**
     * Adds one constraint violation with the given message.
     */
    private static void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
