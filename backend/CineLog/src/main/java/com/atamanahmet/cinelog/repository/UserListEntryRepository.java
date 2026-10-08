package com.atamanahmet.cinelog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atamanahmet.cinelog.domain.entity.ListType;
import com.atamanahmet.cinelog.domain.entity.TmdbMediaType;
import com.atamanahmet.cinelog.domain.entity.UserListEntry;

@Repository
public interface UserListEntryRepository extends JpaRepository<UserListEntry, Long> {

    List<UserListEntry> findByUserIdAndMediaTypeAndListType(
            Integer userId, TmdbMediaType mediaType, ListType listType);

    boolean existsByUserIdAndTmdbIdAndMediaTypeAndListType(
            Integer userId, Integer tmdbId, TmdbMediaType mediaType, ListType listType);

    void deleteByUserIdAndTmdbIdAndMediaTypeAndListType(
            Integer userId, Integer tmdbId, TmdbMediaType mediaType, ListType listType);

    /**
     * Bulk delete conflicting list memberships for one user and title in a single statement.
     */
    @Modifying
    @Query("""
            delete from UserListEntry e
            where e.userId = :userId
              and e.tmdbId = :tmdbId
              and e.mediaType = :mediaType
              and e.listType in :listTypes
            """)
    int deleteConflicts(
            @Param("userId") Integer userId,
            @Param("tmdbId") Integer tmdbId,
            @Param("mediaType") TmdbMediaType mediaType,
            @Param("listTypes") Collection<ListType> listTypes);

    /**
     * Insert one list entry, ignoring the duplicate when the unique membership already exists.
     */
    @Modifying
    @Query(value = """
            insert into user_list_entries (user_id, tmdb_id, media_type, list_type, created_at)
            values (:userId, :tmdbId, :mediaType, :listType, now())
            on conflict on constraint uk_user_list_entry do nothing
            """, nativeQuery = true)
    int insertIgnoreConflict(
            @Param("userId") Integer userId,
            @Param("tmdbId") Integer tmdbId,
            @Param("mediaType") String mediaType,
            @Param("listType") String listType);

    List<UserListEntry> findByUserIdAndListType(Integer userId, ListType listType);

    /**
     * Distinct tmdb_id and media_type for one user and the given list types.
     * Null mediaType means both MOVIE and TV.
     */
    @Query("""
            select distinct e.tmdbId as tmdbId, e.mediaType as mediaType
            from UserListEntry e
            where e.userId = :userId
              and e.listType in :listTypes
              and (:mediaType is null or e.mediaType = :mediaType)
            """)
    List<MediaKeyView> findDistinctMediaKeysByUserIdAndListTypeIn(
            @Param("userId") Integer userId,
            @Param("listTypes") Collection<ListType> listTypes,
            @Param("mediaType") TmdbMediaType mediaType);
}
