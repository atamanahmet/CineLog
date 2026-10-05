package com.atamanahmet.cinelog.service;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import com.atamanahmet.cinelog.dto.CurrentUserResponse;
import com.atamanahmet.cinelog.dto.LoginRequest;
import com.atamanahmet.cinelog.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    ResponseEntity<String> registerUser(RegisterRequest request, HttpServletResponse response);

    ResponseEntity<String> loginUser(LoginRequest request, BindingResult bindingResult, HttpServletResponse response);

    ResponseEntity<CurrentUserResponse> getCurrentUser(HttpServletRequest request);

    /**
     * Rotate refresh token and issue a new access cookie. Authenticated by refresh cookie only.
     */
    ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response);
}
