package com.atamanahmet.cinelog.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RegisterRequestValidationTest {

    private static final String USERNAME = "alice";
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Collects password constraint messages for a register request.
     */
    private static Set<String> passwordMessages(String password) {
        return validator.validate(new RegisterRequest(USERNAME, password)).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void sevenCharactersFailsLengthAlone() {
        assertEquals(Set.of("Password must be at least 8 characters"), passwordMessages("Aa1!xyz"));
    }

    @Test
    void eightCharactersPassesLength() {
        assertEquals(Set.of(), passwordMessages("Aa1!xyzz"));
    }

    @Test
    void seventyTwoAsciiBytesPass() {
        String password = "Aa1!" + "x".repeat(68);
        assertEquals(72, password.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(Set.of(), passwordMessages(password));
    }

    @Test
    void seventyThreeAsciiBytesFail() {
        String password = "Aa1!" + "x".repeat(69);
        assertEquals(73, password.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(Set.of("Password is too long"), passwordMessages(password));
    }

    @Test
    void seventyTwoMultibyteBytesPass() {
        String password = "Aa1!\u00E9" + "x".repeat(66);
        assertEquals(72, password.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(Set.of(), passwordMessages(password));
    }

    @Test
    void seventyThreeMultibyteBytesFail() {
        String password = "Aa1!\u00E9" + "x".repeat(67);
        assertEquals(73, password.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(Set.of("Password is too long"), passwordMessages(password));
    }

    @Test
    void missingUppercaseFailsAlone() {
        assertEquals(Set.of("Password must contain an uppercase letter"), passwordMessages("n7km2wqx!"));
    }

    @Test
    void missingLowercaseFailsAlone() {
        assertEquals(Set.of("Password must contain a lowercase letter"), passwordMessages("N7KM2WQX!"));
    }

    @Test
    void missingDigitFailsAlone() {
        assertEquals(Set.of("Password must contain a number"), passwordMessages("NkMwQxab!"));
    }

    @Test
    void missingSpecialFailsAlone() {
        assertEquals(Set.of("Password must contain a special character"), passwordMessages("N7km2wQx"));
    }

    @Test
    void unicodeUppercaseLetterCountsAsUppercase() {
        assertEquals(Set.of(), passwordMessages("\u00C4pple9!x"));
    }

    @Test
    void blocklistIsCaseInsensitive() {
        assertEquals(Set.of("This password is too common"), passwordMessages("pAsSwOrD1!"));
    }

    @Test
    void password1BangIsRejectedAsTooCommon() {
        assertEquals(Set.of("This password is too common"), passwordMessages("Password1!"));
    }

    @Test
    void strongPasswordPasses() {
        assertEquals(Set.of(), passwordMessages("N7k#mP2wQx!"));
    }

    @Test
    void severalBrokenRulesGiveSeveralMessages() {
        assertEquals(Set.of(
                "Password must be at least 8 characters",
                "Password must contain a number",
                "Password must contain a special character"), passwordMessages("Zz"));
    }
}
