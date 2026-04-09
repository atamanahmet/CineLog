package com.atamanahmet.cinelog.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atamanahmet.cinelog.Utility.ListSelector;
import com.atamanahmet.cinelog.domain.DTO.UserDTO;
import com.atamanahmet.cinelog.domain.POJO.Movie;
import com.atamanahmet.cinelog.domain.POJO.TvShow;
import com.atamanahmet.cinelog.domain.POJO.User;
import com.atamanahmet.cinelog.security.AuthResponse;
import com.atamanahmet.cinelog.security.JwtCookieUtil;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.service.MovieService;
import com.atamanahmet.cinelog.service.TvShowService;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {

    @Value("${recommendation.engine.url}")
    private String recEngineUrl;

    private final MovieService movieService;

    private final UserService userService;

    private final JwtUtil jwtUtil;

    private final JwtCookieUtil jwtCookieUtil;

    private final RestTemplate restTemplate;

    private final TvShowService tvShowService;

    @GetMapping("/signup")
    public String getForm() {
        return new String(
                "To register: POST 127.0.0.1:8080/register, Body={\"username\":\"{yourUsername}\",\"password\":\"{yourPassword}\"}");
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user, HttpServletResponse response) {
        try {
            AuthResponse authResponse = userService.saveUser(user.getUsername(), user.getPassword());

            if (authResponse == null)
                return new ResponseEntity<>("Register error.", HttpStatus.EXPECTATION_FAILED);

            jwtCookieUtil.addJwtCookie(response, authResponse.getToken());

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.ALREADY_REPORTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody User user, BindingResult bindingResult,
            HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        try {
            AuthResponse authResponse = userService.authUser(user);

            if (authResponse == null) {
                return new ResponseEntity<>("User not found. Please register.", HttpStatus.NOT_FOUND);
            }

            jwtCookieUtil.addJwtCookie(response, authResponse.getToken());

            return new ResponseEntity<>(authResponse.getUsername(), HttpStatus.OK);

        } catch (Exception e) {
            System.out.println("error : " + e.getLocalizedMessage());
            return new ResponseEntity<>("Authentication Failed.", HttpStatus.NOT_FOUND);
        }
    }

    public boolean updateList(String action, Set<Integer> idListToEdit, int mediaId) {
        try {
            if (action.equals("add")) {
                if (!idListToEdit.contains(mediaId)) {
                    idListToEdit.add(mediaId);
                }

            } else if (action.equals("del")) {
                if (idListToEdit.contains(mediaId)) {
                    idListToEdit.remove(mediaId);
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @GetMapping("/user/list/{mediaType}/{listType}/{id}/{action}")
    public ResponseEntity<?> addToWatchList(
            @PathVariable(name = "id") String id,
            @PathVariable(name = "mediaType") String mediaType,
            @PathVariable(name = "listType") String listType,
            @PathVariable(name = "action") String action,
            HttpServletRequest request) {

        String username = jwtUtil.extractUsernameFromRequest(request);

        int mediaId = Integer.valueOf(id);

        if (username == null || id == null || action == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        User user = userService.loadByUserName(username);

        Movie movie = new Movie();
        TvShow tvShow = new TvShow();

        Set<Integer> idListToEdit;
        try {
            idListToEdit = ListSelector.getIdListToEdit(user, mediaType, listType);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        switch (mediaType) {
            case "movie":
                movie = movieService.findMovieById(mediaId);
                break;
            case "tv":
                tvShow = tvShowService.findTvShowById(mediaId);
                break;
            default:
                return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }

        if (user == null || (mediaType.equals("movie") ? movie == null : tvShow == null)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        updateList(action, idListToEdit, (mediaType.equals("movie")) ? movie.getId() : tvShow.getId());

        userService.updateUser(user);

        String response = "List edited succesfully";

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/me")
    public ResponseEntity<String> getCurrentUser(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        Cookie cookie = jwtCookieUtil.getJWTCookie(request);

        if (cookie != null) {

            String token = cookie.getValue();

            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return new ResponseEntity<>(username, HttpStatus.OK);

            }
            return new ResponseEntity<>(username, HttpStatus.OK);
        }
        return new ResponseEntity<>("cookie is null", HttpStatus.OK);
    }

    @GetMapping("/user/lists")
    public ResponseEntity<?> getUserArchive(HttpServletRequest request) {
        User user = userService.getUserFromRequest(request);
        if (user == null)
            return new ResponseEntity<>("User Not found", HttpStatus.NOT_FOUND);

        UserDTO userDTO = new UserDTO(
                user.getUserList().getMovieWatchlist(),
                user.getUserList().getMovieWatchedlist(),
                user.getUserList().getMovieLovedlist(),
                getMoviesFromIdSet(user.getUserList().getMovieWatchlist()),
                getMoviesFromIdSet(user.getUserList().getMovieWatchedlist()),
                getMoviesFromIdSet(user.getUserList().getMovieLovedlist()),
                user.getRecommendation());// todo remove

        return new ResponseEntity<UserDTO>(userDTO, HttpStatus.OK);
    }

    @PostMapping("/user/upload")
    public ResponseEntity<?> uploadPhoto(@RequestParam("profilePicture") MultipartFile file,
            HttpServletRequest request) throws IOException {

        if (file == null) {
            return new ResponseEntity<>("File is null", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !List.of("image/jpeg", "image/png").contains(contentType))
            return ResponseEntity.badRequest().body("Unsupported file type");

        String extention = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> null;
        };

        if (extention == null) {
            return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        User user = userService.getUserFromRequest(request);

        if (user != null) {

            System.out.println("Photo upload requested");

            String uploadDir = "uploads/profile-pictures/";

            String uuid = UUID.randomUUID().toString();

            String timestamp = String.valueOf(System.currentTimeMillis());

            String fileName = uuid + "_" + timestamp + extention;

            Path filePath = Paths.get(uploadDir + fileName);

            try {
                Files.createDirectories(filePath.getParent());
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                user.setProfilePicturePath(fileName);

                userService.updateUser(user);

                System.out.println("Photo upload successfull");

                return new ResponseEntity<>(HttpStatus.OK);

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
            }
        }
        return new ResponseEntity<>("User not found. Please login.", HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/user/photo")
    public ResponseEntity<?> getProfilePhoto(HttpServletRequest request) {
        User user = userService.getUserFromRequest(request);

        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String fileName = user.getProfilePicturePath();

        if (fileName == null)
            return ResponseEntity.notFound().build();

        Path path = Paths.get("uploads/profile-pictures/").resolve(fileName);

        if (!Files.exists(path))
            return ResponseEntity.notFound().build();

        String contentType = "";

        try {
            contentType = Files.probeContentType(path);

        } catch (IOException e) {
            System.out.println("Cannot access content type");
            e.printStackTrace();
        }
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new UrlResource(path.toUri()));
        } catch (MalformedURLException e) {
            System.out.println("Url corrupted?");
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    }

    @GetMapping("/user/recommendation")
    public ResponseEntity<String> getRecommendation(HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = "1") String page) {
        return new ResponseEntity<>(sendLovedlistToRec(request, page), HttpStatus.OK);
    }

    public String sendLovedlistToRec(HttpServletRequest request, String page) {
        User user = userService.getUserFromRequest(request);
        ObjectMapper mapper = new ObjectMapper();

        if (user == null) {
            System.out.println("User not exist");
        } else {

            Set<Movie> lovedSet = getMoviesFromIdSet(user.getUserList().getMovieLovedlist());

            if (lovedSet != null) {
                List<Map<String, Object>> movieList = new ArrayList<>();

                for (Movie m : lovedSet) {
                    Map<String, Object> movieMap = new HashMap<>();
                    movieMap.put("id", m.getId());
                    movieMap.put("title", m.getTitle());
                    movieMap.put("overview", m.getOverview());
                    movieMap.put("vote_average", m.getVote_average());
                    movieMap.put("genre_ids", m.getGenre_ids());
                    movieMap.put("release_date", m.getRelease_date().toString());
                    movieList.add(movieMap);
                    System.out.println(m.getTitle());
                }

                try {
                    String json = mapper.writeValueAsString(movieList);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>(json, headers);

                    ResponseEntity<String> res = restTemplate.postForEntity(recEngineUrl, entity, String.class);

                    List<Movie> recList = mapper.readValue(res.getBody(), new TypeReference<List<Movie>>() {
                    });

                    user.setRecommendation(recList);
                    userService.updateUser(user);

                    return mapper.writeValueAsString(user.getRecommendation());
                } catch (Exception e) {
                    System.out.println(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
        return "Content cannot be found ";
    }

    public Set<Movie> getMoviesFromIdSet(Set<Integer> idSet) {
        return new HashSet<Movie>(movieService.getMoviesFromIdSet(idSet));
    }

}
