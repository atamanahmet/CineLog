package com.atamanahmet.cinelog.mapper;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;
import com.atamanahmet.cinelog.domain.entity.Movie;
import com.atamanahmet.cinelog.dto.MovieDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbMovieResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPagedResponse;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = MediaMappingHelper.class,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MovieMapper {

    @Mapping(target = "genreIds", expression = "java(mediaMappingHelper.resolveGenreIds(response.getGenreIds(), response.getGenres()))")
    Movie toEntity(TmdbMovieResponse response);

    List<Movie> toEntities(List<TmdbMovieResponse> responses);

    /**
     * Map a TMDB movie payload to a DTO.
     */
    @Mapping(target = "genreIds", expression = "java(mediaMappingHelper.resolveGenreIds(response.getGenreIds(), response.getGenres()))")
    MovieDto toDto(TmdbMovieResponse response);

    MovieDto toDto(Movie movie);

    List<MovieDto> toDtos(List<Movie> movies);

    /**
     * Map a TMDB paged response to movie entities.
     */
    default List<Movie> fromPage(TmdbPagedResponse<TmdbMovieResponse> page) {
        if (page == null || page.getResults() == null) {
            throw new TmdbClientException("movie", "paged response",
                    "TMDB returned an unexpected response shape: results was null", null);
        }
        return toEntities(page.getResults());
    }

    /**
     * Return an empty list when the source collection is missing.
     */
    default List<Integer> mapIntegerList(List<Integer> source) {
        return source == null ? List.of() : source;
    }
}
