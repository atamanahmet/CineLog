package com.atamanahmet.cinelog.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.dto.UserDTO;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    /**
     * Archive mapping passes through MediaKey id sets and MediaListItemDTO rows.
     */
    @Test
    void toDtoMapsNestedMovieAndEmptyGenreList() {
        User user = new User();

        MediaKey key = new MediaKey(42, TmdbMediaType.MOVIE);
        MediaListItemDTO item = new MediaListItemDTO(
                42,
                "Heat",
                "Heat",
                "/p.jpg",
                LocalDate.of(1995, 12, 15),
                null,
                TmdbMediaType.MOVIE);

        UserDTO dto = mapper.toDto(
                user,
                Set.of(key),
                Set.of(),
                Set.of(),
                Set.of(item),
                Set.of(),
                Set.of(),
                java.util.List.of());

        assertEquals(Set.of(key), dto.watchlistIdSet());
        assertTrue(dto.watchedlistIdSet().isEmpty());
        assertTrue(dto.lovedlistIdSet().isEmpty());
        assertTrue(dto.watchedlist().isEmpty());
        assertTrue(dto.lovedlist().isEmpty());
        assertTrue(dto.recommendation().isEmpty());
        MediaListItemDTO nested = dto.watchlist().iterator().next();
        assertEquals(42, nested.id());
        assertEquals("Heat", nested.title());
        assertEquals("/p.jpg", nested.posterPath());
        assertEquals(LocalDate.of(1995, 12, 15), nested.releaseDate());
        assertEquals(TmdbMediaType.MOVIE, nested.mediaType());
    }
}
