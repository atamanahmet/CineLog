package com.atamanahmet.cinelog.mapper;

import java.util.List;
import java.util.Set;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

import com.atamanahmet.cinelog.domain.entity.MediaKey;
import com.atamanahmet.cinelog.domain.entity.User;
import com.atamanahmet.cinelog.dto.MediaListItemDTO;
import com.atamanahmet.cinelog.dto.RecommendationItemDTO;
import com.atamanahmet.cinelog.dto.UserDTO;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface UserMapper {

    /**
     * Map a user and hydrated list / recommendation rows to the archive DTO.
     * Recommendation must be pre-hydrated from User.recommendation MediaKeys.
     */
    @Mapping(target = "recommendation", source = "recommendation")
    UserDTO toDto(
            User user,
            Set<MediaKey> watchlistIdSet,
            Set<MediaKey> watchedlistIdSet,
            Set<MediaKey> lovedlistIdSet,
            Set<MediaListItemDTO> watchlist,
            Set<MediaListItemDTO> watchedlist,
            Set<MediaListItemDTO> lovedlist,
            List<RecommendationItemDTO> recommendation);
}
