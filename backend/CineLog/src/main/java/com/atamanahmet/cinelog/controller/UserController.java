package com.atamanahmet.cinelog.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.service.CurrentUserService;
import com.atamanahmet.cinelog.service.ProfilePhotoService;
import com.atamanahmet.cinelog.service.RecommendationService;
import com.atamanahmet.cinelog.service.UserListService;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserListService userListService;
    private final RecommendationService recommendationService;
    private final ProfilePhotoService profilePhotoService;
    private final CurrentUserService currentUserService;

    /**
     * Add a media item to a user list.
     */
    @PutMapping("/user/list/{mediaType}/{listType}/{id}")
    public ResponseEntity<?> addToList(
            @PathVariable(name = "id") int id,
            @PathVariable(name = "mediaType") String mediaType,
            @PathVariable(name = "listType") ListType listType) {
        return userListService.addToList(id, resolveMediaType(mediaType), listType);
    }

    /**
     * Remove a media item from a user list.
     */
    @DeleteMapping("/user/list/{mediaType}/{listType}/{id}")
    public ResponseEntity<?> removeFromList(
            @PathVariable(name = "id") String id,
            @PathVariable(name = "mediaType") String mediaType,
            @PathVariable(name = "listType") ListType listType,
            HttpServletRequest request) {
        return userListService.removeFromList(id, resolveMediaType(mediaType), listType, request);
    }

    /**
     * Hydrated items for one list type for the current user.
     */
    @GetMapping("/user/list/{listType}")
    public ResponseEntity<Set<MediaListItemDTO>> getList(
            @PathVariable(name = "listType") ListType listType) {
        Integer userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userListService.getListItems(userId, listType));
    }

    @GetMapping("/user/lists")
    public ResponseEntity<?> getUserArchive(HttpServletRequest request) {
        return userService.getUserArchive(request);
    }

    @PostMapping("/user/upload")
    public ResponseEntity<?> uploadPhoto(@RequestParam("profilePicture") MultipartFile file) {
        return profilePhotoService.uploadPhoto(file);
    }

    @DeleteMapping("/user/photo")
    public ResponseEntity<Void> deletePhoto() {
        return profilePhotoService.deletePhoto();
    }

    /**
     * Scored recommendation rows for one media type. Empty list when there are no loved titles or no hits.
     */
    @GetMapping("/user/recommendation")
    public ResponseEntity<List<RecommendationItemDTO>> getRecommendation(
            @RequestParam(name = "mediaType") TmdbMediaType mediaType,
            @RequestParam(name = "genreInclude", required = false) List<Integer> genreInclude,
            @RequestParam(name = "genreExclude", required = false) List<Integer> genreExclude) {
        return ResponseEntity.ok(
                recommendationService.getRecommendation(mediaType, genreInclude, genreExclude));
    }

    /**
     * Map path token movie/tv to enum. Unknown tokens are 400.
     */
    private static TmdbMediaType resolveMediaType(String raw) {
        if (raw != null) {
            if (raw.equalsIgnoreCase("movie")) {
                return TmdbMediaType.MOVIE;
            }
            if (raw.equalsIgnoreCase("tv")) {
                return TmdbMediaType.TV;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mediaType: " + raw);
    }
}
