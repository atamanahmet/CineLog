package com.atamanahmet.cinelog.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Add, remove and read user list entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserListService {

    static final Map<ListType, Set<ListType>> CONFLICTS = Map.of(
            ListType.REJECTED, Set.of(ListType.WATCHLIST, ListType.LOVED),
            ListType.WATCHLIST, Set.of(ListType.REJECTED),
            ListType.LOVED, Set.of(ListType.REJECTED),
            ListType.WATCHED, Set.of());

    private final UserListEntryRepository userListEntryRepository;
    private final MovieService movieService;
    private final TvShowService tvShowService;
    private final CurrentUserService currentUserService;
    private final ListItemHydrator listItemHydrator;

    /**
     * Add a media id to the matching user list. Skips insert when the id is already present.
     */
    @Transactional
    public ResponseEntity<?> addToList(String id, TmdbMediaType mediaType, ListType listType,
            HttpServletRequest request) {
        if (id == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        int mediaId = Integer.valueOf(id);
        User user = currentUserService.getCurrentUser();
        Movie movie = null;
        TvShow tvShow = null;
        switch (mediaType) {
            case MOVIE:
                try {
                    movie = movieService.findOrFetchMovie(mediaId);
                } catch (DataIntegrityViolationException e) {
                    movie = movieService.findMovieById(mediaId);
                }
                break;
            case TV:
                try {
                    tvShow = tvShowService.findOrFetchTvShow(mediaId);
                } catch (DataIntegrityViolationException e) {
                    tvShow = tvShowService.findTvShowById(mediaId);
                }
                break;
        }
        if (mediaType == TmdbMediaType.MOVIE ? movie == null : tvShow == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        int tmdbId = mediaType == TmdbMediaType.MOVIE ? movie.getId() : tvShow.getId();
        removeConflicts(user.getId(), tmdbId, mediaType, listType);
        if (!userListEntryRepository.existsByUserIdAndTmdbIdAndMediaTypeAndListType(
                user.getId(), tmdbId, mediaType, listType)) {
            UserListEntry entry = new UserListEntry();
            entry.setUserId(user.getId());
            entry.setTmdbId(tmdbId);
            entry.setMediaType(mediaType);
            entry.setListType(listType);
            userListEntryRepository.save(entry);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a media id from the matching user list. Succeeds when the id is already absent.
     */
    @Transactional
    public ResponseEntity<?> removeFromList(String id, TmdbMediaType mediaType, ListType listType,
            HttpServletRequest request) {
        if (id == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        int mediaId = Integer.valueOf(id);
        User user = currentUserService.getCurrentUser();
        Movie movie = null;
        TvShow tvShow = null;
        switch (mediaType) {
            case MOVIE:
                movie = movieService.findMovieById(mediaId);
                break;
            case TV:
                tvShow = tvShowService.findTvShowById(mediaId);
                break;
        }
        if (mediaType == TmdbMediaType.MOVIE ? movie == null : tvShow == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        int tmdbId = mediaType == TmdbMediaType.MOVIE ? movie.getId() : tvShow.getId();
        userListEntryRepository.deleteByUserIdAndTmdbIdAndMediaTypeAndListType(
                user.getId(), tmdbId, mediaType, listType);
        return ResponseEntity.noContent().build();
    }

    /**
     * Hydrated items for one list type for the given user, movie and TV mixed.
     */
    @Transactional(readOnly = true)
    public Set<MediaListItemDTO> getListItems(Integer userId, ListType listType) {
        Set<MediaKey> keys = userListEntryRepository.findByUserIdAndListType(userId, listType).stream()
                .map(entry -> new MediaKey(entry.getTmdbId(), entry.getMediaType()))
                .collect(Collectors.toCollection(HashSet::new));
        return listItemHydrator.hydrate(keys);
    }

    /**
     * Delete conflicting list memberships for one title before add.
     */
    private void removeConflicts(
            Integer userId, Integer tmdbId, TmdbMediaType mediaType, ListType listType) {
        Set<ListType> conflicts = CONFLICTS.getOrDefault(listType, Set.of());
        if (conflicts.isEmpty()) {
            return;
        }
        userListEntryRepository.deleteByUserIdAndTmdbIdAndMediaTypeAndListTypeIn(
                userId, tmdbId, mediaType, conflicts);
    }
}
