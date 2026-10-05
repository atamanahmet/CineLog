/**
 * Recommendation DB: catalog_embedding table and app roles.
 * After apply, for each role run:
 *   ALTER ROLE app_rec_engine WITH LOGIN PASSWORD '<set by hand>';
 *   ALTER ROLE app_embedding_script WITH LOGIN PASSWORD '<set by hand>';
 * Roles created by SQL may not show in the Neon console.
 */

CREATE TABLE IF NOT EXISTS catalog_embedding (
    tmdb_id    integer      NOT NULL,
    media_type text         NOT NULL,
    vector     bytea        NOT NULL,
    model      smallint     NOT NULL,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (tmdb_id, media_type),
    CONSTRAINT catalog_embedding_media_type_check
        CHECK (media_type IN ('MOVIE', 'TV'))
);

COMMENT ON TABLE catalog_embedding IS 'Precomputed catalog embeddings for recommendations.';
COMMENT ON COLUMN catalog_embedding.model IS '1 = BAAI/bge-small-en-v1.5, 384 dim, float16, L2 normalized';
COMMENT ON COLUMN catalog_embedding.vector IS 'float16 bytes, 768 bytes per row for model 1';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_rec_engine') THEN
        CREATE ROLE app_rec_engine NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_embedding_script') THEN
        CREATE ROLE app_embedding_script NOLOGIN;
    END IF;
END
$$;

REVOKE ALL ON TABLE catalog_embedding FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO app_rec_engine, app_embedding_script;
GRANT SELECT ON TABLE catalog_embedding TO app_rec_engine;
GRANT SELECT, INSERT, UPDATE ON TABLE catalog_embedding TO app_embedding_script;
