package com.atamanahmet.cinelog.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.repository.UserListEntryRepository;
import com.atamanahmet.cinelog.security.UserUtil;

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
     * Add a media id to the matching user list. Clears conflicting lists and inserts once,
     * ignoring an existing membership. Fetches the title from TMDB only when it is missing.
     */
    @Transactional
    public ResponseEntity<?> addToList(int id, TmdbMediaType mediaType, ListType listType) {
        Integer userId = UserUtil.getCurrentUserId();
        boolean present = switch (mediaType) {
            case MOVIE -> movieService.existsMovie(id);
            case TV -> tvShowService.existsTvShow(id);
        };
        if (!present) {
            boolean fetched = switch (mediaType) {
                case MOVIE -> movieService.findOrFetchMovie(id) != null;
                case TV -> tvShowService.findOrFetchTvShow(id) != null;
            };
            if (!fetched) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        removeConflicts(userId, id, mediaType, listType);
        userListEntryRepository.insertIgnoreConflict(userId, id, mediaType.name(), listType.name());
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
     * Delete conflicting list memberships for one title before add. WATCHED has no conflicts.
     */
    private void removeConflicts(
            Integer userId, int tmdbId, TmdbMediaType mediaType, ListType listType) {
        Set<ListType> conflicts = CONFLICTS.getOrDefault(listType, Set.of());
        if (conflicts.isEmpty()) {
            return;
        }
        userListEntryRepository.deleteConflicts(userId, tmdbId, mediaType, conflicts);
    }
}
