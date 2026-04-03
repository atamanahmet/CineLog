package com.atamanahmet.cinelog.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atamanahmet.cinelog.domain.SearchEntry;
import com.atamanahmet.cinelog.domain.DTO.MovieVideoDTO;
import com.atamanahmet.cinelog.domain.POJO.Cast;
import com.atamanahmet.cinelog.domain.POJO.CastMember;
import com.atamanahmet.cinelog.domain.POJO.Movie;
import com.atamanahmet.cinelog.domain.POJO.Root;
import com.atamanahmet.cinelog.domain.POJO.TvRoot;
import com.atamanahmet.cinelog.domain.POJO.TvShow;
import com.atamanahmet.cinelog.domain.POJO.User;
import com.atamanahmet.cinelog.domain.POJO.VideoMetadata;
import com.atamanahmet.cinelog.security.JwtUtil;
import com.atamanahmet.cinelog.service.MovieService;
import com.atamanahmet.cinelog.service.TvShowService;
import com.atamanahmet.cinelog.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
class MovieController {
        LocalDate localDate = LocalDate.now().minusDays(1);

        private final String movieUrl = "https://api.themoviedb.org/3/discover/movie?";
        private final String tvUrl = "https://api.themoviedb.org/3/discover/tv?";

        @Value("${api.key}")
        private String apiKey;

        @Autowired
        MovieService movieService;
        @Autowired
        TvShowService tvShowService;

        @Autowired
        UserService userService;

        @Autowired
        private JwtUtil jwtUtil;

        @GetMapping({ "/movie", "/" })
        public ResponseEntity<?> getMovieData(
                        @RequestParam(value = "adult", defaultValue = "false") String adult,
                        @RequestParam(value = "releaseWindow", defaultValue = "") String releaseWindow,
                        @RequestParam(value = "vote_count", defaultValue = "500") String vote_count,
                        @RequestParam(value = "sort", defaultValue = "popularity.desc") String sort,
                        @RequestParam(value = "page", defaultValue = "1") String page,
                        @RequestParam(value = "genreIdList", defaultValue = "") String genreIdList,
                        @RequestParam(value = "yearRange", defaultValue = "1900,2040") int[] yearRange,
                        @RequestParam(value = "ratingRange", defaultValue = "0,10") int[] ratingRange,
                        @RequestParam(value = "languages", defaultValue = "en") String languages)
                        throws IOException, InterruptedException {

                System.out.println("new page:" + page);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(movieUrl + "include_adult=false"
                                                + "&include_video=false"
                                                + "&page=" + page
                                                + "&sort_by=" + sort
                                                + "&vote_count.gte=" + vote_count
                                                + "&with_genres=" + genreIdList
                                                + "&primary_release_date.gte=" + yearRange[0] + "-01-01"
                                                + "&primary_release_date.lte=" + yearRange[1] + "-01-01"
                                                + "&vote_average.gte=" + ratingRange[0]
                                                + "&vote_average.lte=" + ratingRange[1]
                                                + "&with_original_language=" + languages))
                                .header("accept", "application/json")
                                .header("Authorization",
                                                "Bearer " + apiKey)
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                                HttpResponse.BodyHandlers.ofString());

                ObjectMapper mapper = new ObjectMapper();

                Root root = mapper.readValue(response.body(), Root.class);

                fixPosterPath(root);

                String result = mapper.writeValueAsString(root.results);
                return new ResponseEntity<>(result, HttpStatus.OK);
        }

        @GetMapping("/tv")
        public ResponseEntity<String> getTv(@RequestParam(value = "adult", defaultValue = "false") String adult,
                        @RequestParam(value = "releaseWindow", defaultValue = "") String releaseWindow,
                        @RequestParam(value = "vote_count", defaultValue = "500") String vote_count,
                        @RequestParam(value = "sort", defaultValue = "vote_count.desc") String sort,
                        @RequestParam(value = "page", defaultValue = "1") String page,
                        @RequestParam(value = "genreIdList", defaultValue = "") String genreIdList)
                        throws IOException, InterruptedException {

                System.out.println("new TV page request. page: " + page + " adult: " + adult);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(tvUrl + "include_adult=false"
                                                + "&include_video=false&with_original_language=en&page="
                                                + page + "&sort_by=" + sort +
                                                "&vote_count.gte=" + vote_count))
                                .header("accept", "application/json")
                                .header("Authorization",
                                                "Bearer " + apiKey)
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                                HttpResponse.BodyHandlers.ofString());

                ObjectMapper mapper = new ObjectMapper();

                TvRoot tvRoot = mapper.readValue(response.body(), TvRoot.class);

                for (TvShow tvShow : tvRoot.results) { // redirecting poster paths to the image url

                        if (tvShow.getPoster_path() == null) {
                                System.out.println("No poster available" + tvShow.getPoster_path());

                        } else {
                                tvShow.setPoster_path("https://image.tmdb.org/t/p/original" +
                                                tvShow.getPoster_path());
                                tvShow.setBackdrop_path("https://image.tmdb.org/t/p/original" +
                                                tvShow.getBackdrop_path());
                        }

                        tvShowService.saveTvShow(tvShow);
                }

                String result = mapper.writeValueAsString(tvRoot.results);
                return new ResponseEntity<>(result, HttpStatus.OK);
        }

        public void fixPosterPath(Root root) {
                for (Movie movie : root.results) { // redirecting poster paths to the image url

                        if (movie.getPoster_path() == null) {
                                System.out.println("No poster available" + movie.getPoster_path());

                        } else {
                                movie.setPoster_path("https://image.tmdb.org/t/p/original" +
                                                movie.getPoster_path());
                                movie.setBackdrop_path("https://image.tmdb.org/t/p/original" +
                                                movie.getBackdrop_path());
                        }

                        movieService.saveMovie(movie);
                }
        }

        @GetMapping("/{mediaType}/{mediaId}/video")
        public ResponseEntity<String> getVideo(@PathVariable(name = "mediaType") String mediaType,
                        @PathVariable(name = "mediaId", required = true) String mediaId) {
                HttpRequest videoRequest = HttpRequest.newBuilder()
                                .uri(URI.create(
                                                "https://api.themoviedb.org/3/" + mediaType + "/" + mediaId
                                                                + "/videos"))
                                .header("accept", "application/json")
                                .header("Authorization",
                                                "Bearer " + apiKey)
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();

                try {
                        HttpResponse<String> videoResponse = HttpClient.newHttpClient().send(videoRequest,
                                        HttpResponse.BodyHandlers.ofString());

                        ObjectMapper mapper = new ObjectMapper();

                        MovieVideoDTO movieVideoDTO = mapper.readValue(videoResponse.body(), MovieVideoDTO.class);

                        String trailer = "https://www.youtube.com/watch?v=";

                        boolean videoExist = false;

                        if (movieVideoDTO == null || movieVideoDTO.getResults() == null) {
                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }
                        for (VideoMetadata video : movieVideoDTO.getResults()) {
                                if (video.getType().toLowerCase().equals("trailer") &&
                                                video.getSite().toLowerCase().equals("youtube")) {
                                        trailer = trailer + video.getKey();
                                        videoExist = true;
                                        break;
                                }
                        }
                        if (!videoExist) {
                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }
                        if (mediaType.equals("movie")) {
                                Movie movie = movieService.findMovieById(movieVideoDTO.getId());
                                if (movie != null && movie.getTrailer_path() == null) {
                                        movie.setTrailer_path(trailer);
                                        movieService.saveMovie(movie);
                                }
                        } else if (mediaType.equals("tv")) {
                                TvShow tvShow = tvShowService.findTvShowById(movieVideoDTO.getId());
                                if (tvShow != null && tvShow.getTrailer_path() == null) {
                                        tvShow.setTrailer_path(trailer);
                                        tvShowService.saveTvShow(tvShow);
                                }
                        }

                        return new ResponseEntity<>(trailer, HttpStatus.OK);

                } catch (IOException e) {
                        e.printStackTrace();
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        @GetMapping("/{mediaType}/{movieId}/credits")
        public ResponseEntity<String> getCredits(
                        @PathVariable(name = "mediaType") String mediaType,
                        @PathVariable(name = "movieId") String movieId) {

                System.out.println(mediaType);
                System.out.println(movieId);
                HttpRequest castRequest = HttpRequest.newBuilder()
                                .uri(URI.create(
                                                "https://api.themoviedb.org/3/" + mediaType + "/" + movieId
                                                                + "/credits"))
                                .header("accept", "application/json")
                                .header("Authorization",
                                                "Bearer " + apiKey)
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();

                try {
                        HttpResponse<String> castResponse = HttpClient.newHttpClient().send(castRequest,
                                        HttpResponse.BodyHandlers.ofString());

                        ObjectMapper mapper = new ObjectMapper();

                        Cast cast = mapper.readValue(castResponse.body(), Cast.class);

                        for (CastMember member : cast.getCast()) {
                                member.setProfilePath("https://image.tmdb.org/t/p/original" +
                                                member.getProfilePath());
                        }
                        if (mediaType.equals("movie")) {
                                Movie existingMovie = movieService.findMovieById(Integer.valueOf(movieId));

                                if (existingMovie != null && cast != null) {
                                        existingMovie.setCast(cast);
                                        movieService.saveMovie(existingMovie);
                                }
                        } else if (mediaType.equals("tv")) {
                                TvShow existingTvShow = tvShowService.findTvShowById(Integer.valueOf(movieId));

                                if (existingTvShow != null && cast != null) {
                                        existingTvShow.setCast(cast);
                                        tvShowService.saveTvShow(existingTvShow);
                                }
                        } else {
                                System.out.println("Wrong path.");
                        }

                        List<CastMember> top16Cast = cast.getCast().stream()
                                        .sorted(Comparator.comparingDouble(CastMember::getPopularity).reversed())
                                        .limit(16)
                                        .collect(Collectors.toList());

                        return new ResponseEntity<>(mapper.writeValueAsString(top16Cast), HttpStatus.OK);

                } catch (IOException e) {
                        e.printStackTrace();
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }

        @GetMapping("/{mediaType}/{mediaId}")
        public ResponseEntity<?> getMovieById(
                        @PathVariable(name = "mediaId", required = true) String mediaId,
                        @PathVariable(name = "mediaType", required = true) String mediaType)
                        throws IOException, InterruptedException {

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(
                                                "https://api.themoviedb.org/3/"
                                                                + mediaType + "/"
                                                                + mediaId))
                                .header("accept", "application/json")
                                .header("Authorization",
                                                "Bearer " + apiKey)
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                                HttpResponse.BodyHandlers.ofString());

                ObjectMapper mapper = new ObjectMapper();

                // db entry
                if (!response.body().isEmpty()) {
                        if (mediaType.equals("movie")) {
                                Movie movie = mapper.readValue(response.body(), Movie.class);
                        } else {
                                TvShow tvShow = mapper.readValue(response.body(), TvShow.class);
                        }
                }
                return new ResponseEntity<>(response.body(), HttpStatus.OK);
        }

        @GetMapping("/api/{mediaType}")
        public List<?> getMovies(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        @PathVariable(name = "mediaType", required = true) String mediaType) {

                return (mediaType.equals("movies") ? movieService.getAllMovies() : tvShowService.getAllTvShows());
        }

        @GetMapping("/{mediaType}/search/{searchQuery}")
        public ResponseEntity<String> searchHandler(
                        @PathVariable(name = "searchQuery", required = true) String searchQuery,
                        @PathVariable(name = "mediaType", required = true) String mediaType,
                        HttpServletRequest request) {

                String username = jwtUtil.extractUsernameFromRequest(request);

                User user = userService.loadByUserName(username);

                if (user != null) {
                        user.getSearchDataWithDate().add(new SearchEntry(searchQuery, LocalDateTime.now()));
                        userService.updateUser(user);
                }

                HttpRequest requestRedirect = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.themoviedb.org/3/search/" + mediaType + "?query="
                                                + searchQuery))
                                .header("accept", "application/json")
                                .header("Authorization",
                                                "Bearer " + apiKey)
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();

                try {
                        HttpResponse<String> response = HttpClient.newHttpClient().send(requestRedirect,
                                        HttpResponse.BodyHandlers.ofString());

                        ObjectMapper mapper = new ObjectMapper();

                        String result = "";

                        switch (mediaType) {
                                case "movie":
                                        Root root = mapper.readValue(response.body(), Root.class);

                                        for (Movie movie : root.results) { // redirecting poster paths to the image url

                                                movie.setPoster_path("https://image.tmdb.org/t/p/original" +
                                                                movie.getPoster_path());
                                                movie.setBackdrop_path("https://image.tmdb.org/t/p/original" +
                                                                movie.getBackdrop_path());

                                                movieService.saveMovie(movie);

                                        }
                                        try {
                                                result = mapper.writeValueAsString(root.results);
                                        } catch (Exception e) {
                                                System.out.println("Error while creating movie database entry.");
                                                System.out.println(e.getLocalizedMessage());
                                        }
                                        break;
                                case "tv":
                                        TvRoot tvRoot = mapper.readValue(response.body(), TvRoot.class);

                                        for (TvShow tvShow : tvRoot.results) { // redirecting poster paths to the image
                                                // url

                                                tvShow.setPoster_path("https://image.tmdb.org/t/p/original" +
                                                                tvShow.getPoster_path());
                                                tvShow.setBackdrop_path("https://image.tmdb.org/t/p/original" +
                                                                tvShow.getBackdrop_path());

                                                tvShowService.saveTvShow(tvShow);
                                        }
                                        try {
                                                result = mapper.writeValueAsString(tvRoot.results);
                                        } catch (Exception e) {
                                                System.out.println("Error while creating tv database entry.");
                                                System.out.println(e.getLocalizedMessage());
                                        }
                                        break;

                                default:
                                        break;
                        }

                        return new ResponseEntity<>(result, HttpStatus.OK);

                } catch (IOException | InterruptedException e) {
                        System.out.println("Search call error");
                        System.out.println(e.getLocalizedMessage());
                        return new ResponseEntity<>(null, HttpStatus.EXPECTATION_FAILED);
                }

        }

}
