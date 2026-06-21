-- Watchlist duplicate cleanup + unique constraint
--
-- This project has no Flyway/Liquibase configured (spring.jpa.hibernate.ddl-auto=update
-- only adds tables/columns it knows about from @Entity mappings; it never creates
-- expression-based unique indexes). This script must be run manually, once, against
-- the target database, BEFORE the unique index exists. After it has been applied,
-- re-running it is safe (idempotent): step 1 finds nothing, step 2 deletes nothing,
-- step 3 fails harmlessly if the index already exists (guarded with IF NOT EXISTS).
--
-- Business rule encoded by the index: a card is uniquely identified by
-- (card_name, edition), where a NULL edition is treated as equivalent to ''.
-- This closes the gap where two rows with the same card_name and edition = NULL
-- would NOT be caught by a plain UNIQUE(card_name, edition) constraint, since
-- PostgreSQL never considers two NULLs equal in a unique constraint.

-- 1. Detect duplicate groups (generic - any number of groups, not just one).
SELECT card_name,
       COALESCE(edition, '') AS edition_normalized,
       COUNT(*)              AS duplicate_count
FROM watchlist
GROUP BY card_name, COALESCE(edition, '')
HAVING COUNT(*) > 1;

-- 2. Keep the oldest row per (card_name, normalized edition) group, delete the rest.
DELETE FROM watchlist w
USING (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY card_name, COALESCE(edition, '')
               ORDER BY created_at ASC, id ASC
           ) AS row_num
    FROM watchlist
) ranked
WHERE w.id = ranked.id
  AND ranked.row_num > 1;

-- 3. Verify no duplicate groups remain (should return zero rows).
SELECT card_name,
       COALESCE(edition, '') AS edition_normalized,
       COUNT(*)              AS duplicate_count
FROM watchlist
GROUP BY card_name, COALESCE(edition, '')
HAVING COUNT(*) > 1;

-- 4. Enforce uniqueness going forward at the database level.
--    NULL and '' editions are normalized to the same value via COALESCE,
--    so this also prevents future NULL-edition duplicates.
CREATE UNIQUE INDEX IF NOT EXISTS uq_watchlist_card_name_edition
    ON watchlist (card_name, COALESCE(edition, ''));
