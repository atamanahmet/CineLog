package com.atamanahmet.cinelog.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CatalogCacheId implements Serializable {

    private Integer tmdbId;

    private TmdbMediaType mediaType;
}
