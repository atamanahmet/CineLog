package com.atamanahmet.cinelog.dto;

import java.util.List;
import java.util.Set;

import com.atamanahmet.cinelog.domain.entity.MediaKey;

public record UserDTO(
        String profilePictureUrl,
        Set<MediaKey> watchlistIdSet,
        Set<MediaKey> watchedlistIdSet,
        Set<MediaKey> lovedlistIdSet,
        Set<MediaListItemDTO> watchlist,
        Set<MediaListItemDTO> watchedlist,
        Set<MediaListItemDTO> lovedlist,
        List<RecommendationItemDTO> recommendation) {
}
