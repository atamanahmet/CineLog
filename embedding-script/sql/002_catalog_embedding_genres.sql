/**
 * Add nullable genre_ids and vote_count to catalog_embedding.
 * NULL means not backfilled. An empty genre_ids array means TMDB lists no genres.
 */

ALTER TABLE catalog_embedding
    ADD COLUMN IF NOT EXISTS genre_ids integer[],
    ADD COLUMN IF NOT EXISTS vote_count integer;

COMMENT ON COLUMN catalog_embedding.genre_ids IS
    'Canonical TMDB genre ids. NULL = not backfilled. Empty = no genres.';
COMMENT ON COLUMN catalog_embedding.vote_count IS
    'TMDB vote_count. NULL = not backfilled.';
