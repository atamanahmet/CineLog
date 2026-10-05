package com.atamanahmet.cinelog.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.atamanahmet.cinelog.dto.PersonCreditDto;
import com.atamanahmet.cinelog.dto.PersonCreditRoleDto;
import com.atamanahmet.cinelog.dto.PersonDetailDto;
import com.atamanahmet.cinelog.dto.PersonInfoDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbCombinedCreditItem;
import com.atamanahmet.cinelog.dto.tmdb.TmdbCombinedCreditsResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPersonResponse;

/**
 * Maps a TMDB person payload with combined credits into the API DTO.
 */
@Component
public class PersonMapper {

    private static final int ALSO_KNOWN_AS_LIMIT = 5;

    /**
     * Build person details and merged credits. Null response becomes null.
     */
    public PersonDetailDto toDetail(TmdbPersonResponse response) {
        if (response == null) {
            return null;
        }
        return new PersonDetailDto(toPersonInfo(response), mergeCredits(response.getCombinedCredits()));
    }

    private PersonInfoDto toPersonInfo(TmdbPersonResponse response) {
        return new PersonInfoDto(
                response.getId(),
                response.getName(),
                response.getBiography(),
                blankToNull(response.getBirthday()),
                blankToNull(response.getDeathday()),
                blankToNull(response.getPlaceOfBirth()),
                response.getProfilePath(),
                blankToNull(response.getKnownForDepartment()),
                capAlsoKnownAs(response.getAlsoKnownAs()));
    }

    /**
     * Merge cast and crew rows by media type and id. Keep TMDB encounter order.
     */
    List<PersonCreditDto> mergeCredits(TmdbCombinedCreditsResponse credits) {
        if (credits == null) {
            return List.of();
        }
        Map<String, MutableCredit> merged = new LinkedHashMap<>();
        appendCredits(merged, credits.getCast(), "cast");
        appendCredits(merged, credits.getCrew(), "crew");
        List<PersonCreditDto> out = new ArrayList<>(merged.size());
        for (MutableCredit credit : merged.values()) {
            out.add(credit.toDto());
        }
        return List.copyOf(out);
    }

    private void appendCredits(Map<String, MutableCredit> merged, List<TmdbCombinedCreditItem> items, String kind) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (TmdbCombinedCreditItem item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            String mediaType = normalizeMediaType(item.getMediaType());
            if (mediaType == null) {
                continue;
            }
            String title = resolveTitle(item, mediaType);
            if (title == null) {
                continue;
            }
            String key = mediaType + ":" + item.getId();
            MutableCredit credit = merged.get(key);
            if (credit == null) {
                credit = new MutableCredit(
                        mediaType,
                        item.getId(),
                        title,
                        item.getPosterPath(),
                        resolveDate(item, mediaType),
                        item.getVoteAverage(),
                        copyGenreIds(item.getGenreIds()));
                merged.put(key, credit);
            }
            credit.roles.add(toRole(item, kind));
        }
    }

    private PersonCreditRoleDto toRole(TmdbCombinedCreditItem item, String kind) {
        if ("crew".equals(kind)) {
            return new PersonCreditRoleDto(
                    "crew",
                    blankToNull(item.getJob()),
                    blankToNull(item.getDepartment()),
                    null);
        }
        Integer episodeCount = "tv".equals(normalizeMediaType(item.getMediaType()))
                ? item.getEpisodeCount()
                : null;
        return new PersonCreditRoleDto(
                "cast",
                blankToNull(item.getCharacter()),
                null,
                episodeCount);
    }

    private static String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            return null;
        }
        String lower = mediaType.trim().toLowerCase();
        if ("movie".equals(lower) || "tv".equals(lower)) {
            return lower;
        }
        return null;
    }

    private static String resolveTitle(TmdbCombinedCreditItem item, String mediaType) {
        if ("tv".equals(mediaType)) {
            return blankToNull(item.getName());
        }
        return blankToNull(item.getTitle());
    }

    private static String resolveDate(TmdbCombinedCreditItem item, String mediaType) {
        if ("tv".equals(mediaType)) {
            return blankToNull(item.getFirstAirDate());
        }
        return blankToNull(item.getReleaseDate());
    }

    private static List<String> capAlsoKnownAs(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            cleaned.add(name);
            if (cleaned.size() == ALSO_KNOWN_AS_LIMIT) {
                break;
            }
        }
        return List.copyOf(cleaned);
    }

    private static List<Integer> copyGenreIds(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return List.of();
        }
        return List.copyOf(genreIds);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static final class MutableCredit {
        private final String mediaType;
        private final Integer tmdbId;
        private final String title;
        private final String posterPath;
        private final String date;
        private final Double voteAverage;
        private final List<Integer> genreIds;
        private final List<PersonCreditRoleDto> roles = new ArrayList<>();

        private MutableCredit(
                String mediaType,
                Integer tmdbId,
                String title,
                String posterPath,
                String date,
                Double voteAverage,
                List<Integer> genreIds) {
            this.mediaType = mediaType;
            this.tmdbId = tmdbId;
            this.title = title;
            this.posterPath = posterPath;
            this.date = date;
            this.voteAverage = voteAverage;
            this.genreIds = genreIds;
        }

        private PersonCreditDto toDto() {
            return new PersonCreditDto(
                    mediaType,
                    tmdbId,
                    title,
                    posterPath,
                    date,
                    voteAverage,
                    genreIds,
                    List.copyOf(roles));
        }
    }
}
