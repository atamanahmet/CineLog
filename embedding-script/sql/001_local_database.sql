/**
 * LOCAL DEV ONLY. Never applied to Neon.
 * Creates the rec database next to the backend database, so local has
 * the same two-database shape as production.
 * Run with: psql -v rec_db=<name>. Safe to run more than once.
 */

SELECT format('CREATE DATABASE %I', :'rec_db')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'rec_db')
\gexec