-- Index for card_price_history lookups
--
-- This project has no Flyway/Liquibase configured (spring.jpa.hibernate.ddl-auto=update
-- only adds tables/columns it knows about from @Entity mappings; it never creates
-- expression-based indexes). This script must be run manually, once, against the
-- target database. It is idempotent (guarded with IF NOT EXISTS), so re-running it
-- is safe.
--
-- Query covered: CardPriceHistoryRepository.findByCardNameIgnoreCaseOrderByCapturedAtAsc(...),
-- the only query against this table in the codebase. Hibernate translates the
-- "IgnoreCase" keyword on a String property into a case-folded predicate. Confirmed
-- against the real generated SQL (spring.jpa.show-sql=true, captured live while
-- running the app against this same database):
--
--   where upper(cphe1_0.card_name)=upper(?)
--
-- i.e. UPPER(), not LOWER() (Hibernate's IgnoreCase default differs by version/dialect,
-- so this must always be checked against the actual logged SQL rather than assumed).
-- A plain B-tree index on card_name would NOT be matched by the query planner against
-- that predicate, so the index below is built on the expression UPPER(card_name)
-- instead of the raw column. captured_at is appended as a second key so the same
-- index also satisfies the query's ORDER BY captured_at without an extra sort step.

CREATE INDEX IF NOT EXISTS idx_card_price_history_name_captured
    ON card_price_history (UPPER(card_name), captured_at);
