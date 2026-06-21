# DECISIONS.md

A log of architecturally-significant decisions visible in the current codebase. None of this
work is committed to git yet (the working tree predates this log), so entries below are ordered
logically rather than by date — dates are not invented where they aren't verifiable.

---

## D1 — Scheduler reads from the watchlist, not from ad-hoc searches

**Decision:** `PriceTrackingScheduler` iterates `WatchlistRepository.findAll()` rather than
tracking whatever cards users happen to search for. `CardComparisonService.compare()` is
read-only and never calls `HistoricalPriceService`.

**Why:** ties storage growth to a bounded, user-curated set of cards instead of unbounded search
traffic, and makes "what gets tracked" an explicit, inspectable list (`GET /api/watchlist`)
instead of an implicit side effect of search.

**Consequence:** `CardComparisonService` still has `HistoricalPriceService` injected as an unused
field — a leftover from before this decision. Harmless, flagged for future cleanup.

---

## D2 — `card_price_history` lookups use an expression index on `UPPER(card_name)`

**Decision:** `card_price_history_index.sql` creates `(UPPER(card_name), captured_at)` rather than
a plain B-tree index on `card_name`.

**Why:** `findByCardNameIgnoreCaseOrderByCapturedAtAsc` compiles (verified against live
`show-sql` output, not assumed) to `WHERE upper(card_name) = upper(?)`. Hibernate's `IgnoreCase`
keyword can compile to `UPPER()` or `LOWER()` depending on version/dialect, so this must be
checked against real generated SQL each time it matters, rather than assumed from documentation.

**Consequence:** `spring.jpa.show-sql` was deliberately left on at the time of this investigation;
it has since been turned back off (see L1 in `AI_USAGE_LOG.md`) since it's not needed for normal
operation.

---

## D3 — No Flyway/Liquibase; manual idempotent SQL scripts instead

**Decision:** Schema changes that `ddl-auto=update` can't express (expression indexes, unique
constraints with `COALESCE`) live as plain `.sql` files under `src/main/resources/sql/`, to be
run by hand once per environment.

**Why:** smallest-footprint option for a project at this size; avoids introducing a migration
framework dependency for two scripts.

**Risk accepted:** these scripts are not run automatically on deploy. A fresh environment without
them applied will work functionally but with degraded query performance (D2) or a real
data-integrity gap (D4) until someone remembers to run them.

---

## D4 — Watchlist uniqueness normalizes `NULL` edition to `''`

**Decision:** `uq_watchlist_card_name_edition` is built on `(card_name, COALESCE(edition, ''))`,
not a plain `UNIQUE(card_name, edition)`.

**Why:** PostgreSQL never considers two `NULL`s equal under a standard unique constraint, so a
plain constraint would silently allow duplicate `(card_name, NULL)` rows. `WatchlistService` also
encodes the same rule in application code (`existsByCardNameAndEditionIsNull` vs.
`existsByCardNameAndEdition`) and treats a `DataIntegrityViolationException` from a lost race
against this index as an idempotent no-op rather than an error.

---

## D5 — `MtgStocksScraper` is a separate bean from `MtgStocksProvider`

**Decision:** HTTP/scraping calls live in `MtgStocksScraper`, matching/business logic in
`MtgStocksProvider`, even though they could be one class.

**Why:** Spring's `@Cacheable` proxy doesn't intercept self-invocation — calling a `@Cacheable`
method from another method on the *same* bean bypasses the cache entirely. Splitting the
HTTP-calling methods into their own bean is what makes the `mtgstocksSetsIndex` /
`mtgstocksSetDetail` caches actually take effect.

---

## D6 — Redis cache deserialization scoped to project DTOs only

**Decision:** `RedisConfig`'s Jackson polymorphic type validator allows only
`com.smichelotti.mtg.dto`, `java.util.*`, and `java.lang.*` subtypes, replacing an earlier
`allowIfSubType(Object.class)` configuration (referenced in code comments as a "Fase 6" security
review).

**Why:** `allowIfSubType(Object.class)` accepts any class on the classpath as a polymorphic
deserialization target from cached JSON — a known gadget-chain deserialization risk if the cache
contents are ever attacker-influenced. Scoping to the project's own DTO package closes that
without losing functionality, since nothing the app caches is outside that package.

---

## D7 — Historical writes are unconditional (no change detection)

**Decision (status quo, not yet changed):** every scheduler tick writes one `card_price_history`
row per provider result, regardless of whether the price differs from the last recorded value.

**Why this is still the case:** confirmed via investigation (Sprint 5B) that real providers
produce 94–98% duplicate-valued rows tick-over-tick, and that storage growth from this is currently
small relative to likely watchlist sizes — not yet worth the complexity of a change-detection
layer.

**Recommended follow-up (not implemented):** change detection before `save()`; pagination/date
range on `/api/cards/history`; a retention policy; partitioning only if/when retention and
pagination prove insufficient. See `AI_USAGE_LOG.md` for the full investigation trail.

---

## D8 — `MockPriceProvider` stays wired into the live provider list

**Decision (status quo):** `MockPriceProvider` (`MockTrader`, random $2–$5 price) is a normal
`@Component` `CardPriceProvider` and runs on every real `/api/cards/compare` call alongside
Scryfall and MTGStocks.

**Why this was investigated, not changed:** Sprint 5A confirmed the historical MockTrader/Scryfall
price discrepancy users saw was caused by card-name casing fragmentation in stored data, not by
MockTrader running more often than the real providers. No code change was made as a result —
this entry exists so the reasoning isn't lost, not because a fix was applied.

---

## D9 — Logging: SLF4J only, level classification by business-significance

**Decision:** INFO for business-significant events, DEBUG for diagnostic/trace detail, WARN for
caught-but-recoverable conditions, ERROR for caught exceptions (always logged with the exception
object). `spring.jpa.show-sql`/`format_sql` default to off.

**Why:** establishes a consistent, intentional convention going forward rather than ad-hoc level
choices; see `AI_USAGE_LOG.md` (Sprint 1) for the cleanup pass that applied this retroactively to
existing log statements.
