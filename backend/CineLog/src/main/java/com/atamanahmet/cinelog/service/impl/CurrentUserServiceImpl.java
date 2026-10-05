package com.atamanahmet.cinelog.service.impl;

import org.springframework.stereotype.Service;

import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.exception.UserNotFoundException;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.security.UserUtil;
import com.atamanahmet.cinelog.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Reloads the authenticated user by id. Caller must provide the transaction.
     */
    @Override
    public User getCurrentUser() {
        Integer id = UserUtil.getCurrentUserId();
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }
}
