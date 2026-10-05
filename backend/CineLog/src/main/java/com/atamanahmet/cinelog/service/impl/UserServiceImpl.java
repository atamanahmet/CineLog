package com.atamanahmet.cinelog.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.dto.UserDTO;
import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.mapper.UserMapper;
import com.atamanahmet.cinelog.exception.UserNotFoundException;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.repository.UserRepository;
import com.atamanahmet.cinelog.service.CurrentUserService;
import com.atamanahmet.cinelog.service.ListItemHydrator;
import com.atamanahmet.cinelog.service.RecommendationExclusionService;
import com.atamanahmet.cinelog.service.RecommendationHydrator;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserListEntryRepository userListEntryRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final RecommendationHydrator recommendationHydrator;
    private final ListItemHydrator listItemHydrator;
    private final RecommendationExclusionService recommendationExclusionService;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserArchive(HttpServletRequest request) {
        User user = currentUserService.getCurrentUser();
        Set<MediaKey> watchlistIds = new HashSet<>();
        watchlistIds.addAll(listIds(user.getId(), TmdbMediaType.MOVIE, ListType.WATCHLIST));
        watchlistIds.addAll(listIds(user.getId(), TmdbMediaType.TV, ListType.WATCHLIST));
        Set<MediaKey> watchedlistIds = new HashSet<>();
        watchedlistIds.addAll(listIds(user.getId(), TmdbMediaType.MOVIE, ListType.WATCHED));
        watchedlistIds.addAll(listIds(user.getId(), TmdbMediaType.TV, ListType.WATCHED));
        Set<MediaKey> lovedlistIds = findLovedKeys(user.getId());
        Set<MediaKey> excluded = recommendationExclusionService.allExcluded(user.getId());
        List<MediaKey> visibleRecommendation = user.getRecommendation().stream()
                .filter(key -> !excluded.contains(key))
                .toList();
        UserDTO userDTO = userMapper.toDto(
                user,
                watchlistIds,
                watchedlistIds,
                lovedlistIds,
                listItemHydrator.hydrate(watchlistIds),
                listItemHydrator.hydrate(watchedlistIds),
                listItemHydrator.hydrate(lovedlistIds),
                recommendationHydrator.hydrateCacheOnly(visibleRecommendation));
        return new ResponseEntity<UserDTO>(userDTO, HttpStatus.OK);
    }

    @Override
    @Transactional(readOnly = true)
    public User loadByUserName(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Loved movie and TV keys for one user.
     */
    @Override
    @Transactional(readOnly = true)
    public Set<MediaKey> findLovedKeys(Integer userId) {
        Set<MediaKey> lovedKeys = new HashSet<>();
        lovedKeys.addAll(listIds(userId, TmdbMediaType.MOVIE, ListType.LOVED));
        lovedKeys.addAll(listIds(userId, TmdbMediaType.TV, ListType.LOVED));
        return lovedKeys;
    }

    /**
     * Replace stored recommendation keys for one media type. Dirty checking persists the change.
     */
    @Override
    @Transactional
    public void replaceRecommendation(Integer userId, TmdbMediaType mediaType, List<MediaKey> keys) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.replaceRecommendation(mediaType, keys == null ? List.of() : keys);
    }

    /**
     * Set the profile photo URL when it changed. Dirty checking persists the change.
     */
    @Override
    @Transactional
    public void updateProfilePhotoUrl(Integer userId, String url) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (Objects.equals(user.getProfilePictureUrl(), url)) {
            return;
        }
        user.setProfilePictureUrl(url);
    }

    private Set<MediaKey> listIds(Integer userId, TmdbMediaType mediaType, ListType listType) {
        return userListEntryRepository.findByUserIdAndMediaTypeAndListType(userId, mediaType, listType).stream()
                .map(entry -> new MediaKey(entry.getTmdbId(), mediaType))
                .collect(Collectors.toSet());
    }

}
