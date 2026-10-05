package com.atamanahmet.cinelog.mapper;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.atamanahmet.cinelog.client.tmdb.TmdbClientException;
import com.atamanahmet.cinelog.domain.entity.TvShow;
import com.atamanahmet.cinelog.dto.TvShowDto;
import com.atamanahmet.cinelog.dto.tmdb.TmdbPagedResponse;
import com.atamanahmet.cinelog.dto.tmdb.TmdbTvShowResponse;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = MediaMappingHelper.class,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TvShowMapper {

    @Mapping(target = "genreIds", expression = "java(mediaMappingHelper.resolveGenreIds(response.getGenreIds(), response.getGenres()))")
    @Mapping(target = "releaseDate", expression = "java(mediaMappingHelper.stringToLocalDate(response.getReleaseDate()))")
    TvShow toEntity(TmdbTvShowResponse response);

    List<TvShow> toEntities(List<TmdbTvShowResponse> responses);

    /**
     * Map a TMDB TV payload to a DTO.
     */
    @Mapping(target = "genreIds", expression = "java(mediaMappingHelper.resolveGenreIds(response.getGenreIds(), response.getGenres()))")
    @Mapping(target = "releaseDate", expression = "java(mediaMappingHelper.stringToLocalDate(response.getReleaseDate()))")
    @Mapping(target = "firstAirDate", expression = "java(mediaMappingHelper.stringToLocalDate(response.getReleaseDate()))")
    @Mapping(target = "lastAirDate", expression = "java(mediaMappingHelper.stringToLocalDate(response.getLastAirDate()))")
    @Mapping(target = "episodeRunTime", source = "episodeRunTime", qualifiedByName = "firstEpisodeRunTime")
    @Mapping(target = "networks", source = "networks", qualifiedByName = "networkNames")
    @Mapping(target = "createdBy", source = "createdBy", qualifiedByName = "mapCreatedBy")
    @Mapping(target = "nextEpisodeToAir", source = "nextEpisodeToAir", qualifiedByName = "mapNextEpisode")
    @Mapping(target = "seasons", source = "seasons", qualifiedByName = "mapSeasons")
    TvShowDto toDto(TmdbTvShowResponse response);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "numberOfSeasons", ignore = true)
    @Mapping(target = "numberOfEpisodes", ignore = true)
    @Mapping(target = "episodeRunTime", ignore = true)
    @Mapping(target = "firstAirDate", ignore = true)
    @Mapping(target = "lastAirDate", ignore = true)
    @Mapping(target = "networks", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "nextEpisodeToAir", ignore = true)
    @Mapping(target = "seasons", ignore = true)
    TvShowDto toDto(TvShow tvShow);

    List<TvShowDto> toDtos(List<TvShow> tvShows);

    /**
     * Map a TMDB paged response to TV show entities.
     */
    default List<TvShow> fromPage(TmdbPagedResponse<TmdbTvShowResponse> page) {
        if (page == null || page.getResults() == null) {
            throw new TmdbClientException("tv", "paged response",
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

    /**
     * Return an empty list when the source collection is missing.
     */
    default List<String> mapStringList(List<String> source) {
        return source == null ? List.of() : source;
    }
}
