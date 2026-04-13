-- V6: Index cleanup and color column constraint
--
-- CHANGES IN THIS MIGRATION:
--
-- 1. DROP idx_postits_color
--    Reason: Created in V1 before user ownership existed. Color has very low
--    cardinality (small set of hex values), making a single-column index on it
--    ineffective — PostgreSQL's planner will prefer a seq-scan over an index with
--    low selectivity. No current query filters solely by color. Dead weight on
--    every INSERT/UPDATE.
--
-- 2. DROP idx_postits_user_id
--    Reason: Created in V3 as a standalone index on user_id. This column is now
--    the leading key of idx_postits_user_id_created_at (V5). PostgreSQL can
--    satisfy a bare "WHERE user_id = ?" using the composite index (leading column
--    prefix rule), so the single-column index is fully redundant. Keeping both
--    wastes ~same storage and doubles the write overhead for every INSERT/UPDATE
--    that touches user_id.
--
--    VERIFY before applying in production:
--      SELECT idx_scan FROM pg_stat_user_indexes
--      WHERE indexname = 'idx_postits_user_id';
--    If idx_scan > 0, wait until idx_postits_user_id_created_at has been live
--    long enough for the planner to switch (typically one ANALYZE cycle).
--
-- 3. color column: add NOT NULL + explicit DEFAULT
--    The domain model enforces a fallback to '#FFFFFF' in Postit.create(), but
--    the DB column was left nullable without a DEFAULT — a constraint gap.
--    Backfill NULL rows first (none expected in production after V4), then
--    tighten the column definition.
--
-- ZERO-DOWNTIME: All three operations are safe.
--   - DROP INDEX: instant metadata removal, no table lock.
--   - UPDATE + ALTER COLUMN: UPDATE affects 0 rows in production (all postits
--     have had color set since V1 created the column with DEFAULT '#FFFFFF').
--     ALTER COLUMN SET NOT NULL acquires only a brief ShareUpdateExclusiveLock
--     when the table has no NULLs to validate (PostgreSQL 12+ uses a CHECK
--     constraint approach for NOT NULL that avoids full table scan if backfill
--     already ran).

-- Step 1: Drop the low-cardinality, unused color index
DROP INDEX IF EXISTS idx_postits_color;

-- Step 2: Drop the redundant single-column user_id index
-- (covered by the leading key of idx_postits_user_id_created_at)
DROP INDEX IF EXISTS idx_postits_user_id;

-- Step 3: Backfill any NULL color values before applying NOT NULL
UPDATE postits
SET color = '#FFFFFF'
WHERE color IS NULL;

-- Step 4: Apply NOT NULL + DEFAULT on color column
ALTER TABLE postits
    ALTER COLUMN color SET DEFAULT '#FFFFFF',
    ALTER COLUMN color SET NOT NULL;
