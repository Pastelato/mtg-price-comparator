# AI_USAGE_LOG.md

A log of AI-assisted work sessions on this repository. Entries are based on what's directly
verifiable (current code state, conversation record); none of this work is committed to git yet,
so calendar dates are omitted where they can't be confirmed against git history.

---

## Gap notice — Sprint 3.5 and Sprint 4

No record of these two sprints' specific scope could be found: `git log` contains no
sprint-numbered commits (recent history is a flat sequence of `Fix Scryfall...` / `Fix MTGStocks
debug print` style messages), and no prior sprint-log documentation existed in this repo before
this entry. Their cumulative effect is visible in the current code and is documented in
`ARCHITECTURE.md`, `PROJECT_MAP.md`, and `DECISIONS.md` — most likely candidates based on what
exists today: the watchlist feature end-to-end, the MTGStocks provider/scraper pair and its
edition-name normalization, the Redis caching layer, and the `RedisConfig` polymorphic-typing
security scoping (D6 in `DECISIONS.md`). This is stated explicitly rather than reconstructed,
since fabricating sprint narratives would defeat the purpose of this log.

---

## Sprint 5A — Investigation: MockTrader vs. Scryfall price discrepancy

**Type:** read-only investigation. No code changed.

**Question:** why did historical data show MockTrader prices appearing disproportionately
relative to real providers?

**Finding:** the discrepancy was caused by **card-name casing fragmentation** in stored
`card_price_history` rows (the same card persisted under inconsistently-cased names, splitting
what should have been one history series into several), not by `MockPriceProvider` executing more
frequently than the real providers. `MockPriceProvider` remains active in the live provider list
(D8 in `DECISIONS.md`) — this investigation explains a symptom, it doesn't argue for removing it.

---

## Sprint 5B — Investigation: duplicate historical rows / storage growth

**Type:** read-only investigation. No code changed.

**Scope:** persistence flow review, real-database analysis, redundant-write scenario mapping,
provider behavior characterization, storage growth projection, query impact, design options,
final recommendation.

**Key findings:**

- Real providers (Scryfall, MTGStocks) generate **94–98% duplicate-valued rows** across
  consecutive scheduler ticks.
- Historical writes are unconditional — confirmed in code (`HistoricalPriceService.save()` has no
  prior-value check) — see D7 in `DECISIONS.md`.
- Storage growth from this is currently small in absolute terms; it becomes a real concern only as
  the watchlist scales up.

**Recommended future work (not implemented):**

1. Change detection before `save()`
2. Pagination / date ranges on `GET /api/cards/history`
3. A retention policy
4. Partitioning — only if 1–3 prove insufficient

---

## Sprint 1 — Logging & Cleanup

**Type:** implementation (logging/cleanup only — no new features, no functional behavior change).

**Findings before any change:**

- `System.out.println` / `System.err` / `printStackTrace`: **0 occurrences** anywhere in `src`.
- Banned temp-comment markers (`TODO DEBUG`, `FORENSIC`, `TEMP`, `TEMPORARY`, `REMOVE BEFORE
  PROD`): **0 occurrences**.
- All 5 catch blocks in `src/main` already used `log.error(..., e)` correctly, except one.

**Changes made (5 files):**

| File | Change |
|---|---|
| `application.properties` | `spring.jpa.show-sql` / `format_sql` turned off (were left on for the D2 investigation) |
| `WatchlistService.java` | 1 log: INFO → WARN (lost-race catch block — an actual caught exception, not a routine no-op) |
| `ScryfallClient.java` | 3 logs: INFO → DEBUG (outbound call traces) |
| `MtgStocksProvider.java` | 6 logs: INFO → DEBUG (internal matching/candidate trace steps) |
| `MtgStocksScraper.java` | 4 logs: INFO → DEBUG (scrape timing/volume traces) |

14 total reclassifications. Genuine business-outcome logs (compare request/resolved, watchlist
add/delete, scheduler cycle start, total-matches/results-returned summaries) were deliberately
left at INFO.

**Verification:** `./mvnw clean compile` → `BUILD SUCCESS` (43 source files). `./mvnw test` →
`BUILD SUCCESS`, **51/51 tests passing**, 0 failures/errors/skipped — confirming no functional
behavior changed (re-verified again during this documentation sprint, same result).

**Risks flagged, not remediated (out of cleanup scope):**

- `CardComparisonController` and `CardComparisonService` both log an INFO line for the same
  incoming compare request — duplicate, not yet deduplicated.
- `JsoupTest.java` and `TestController.java` are dev scaffolding living in `src/main`.
- `ScryfallController` has no logger at all.

---

## Open Investigation — MTGStocks set resolution failures

**Type:** open investigation. Status: **not resolved** — root cause not yet found, no code changed.

**Symptom:** `GET /api/cards/compare` returns zero MTGStocks results for some cards/editions even
though Scryfall results and edition resolution succeed normally for the same request.

**Known failing example:** Time Spiral Remastered (`setCode=tsr`) — `MtgStocksProvider` logs
`SET URL NOT FOUND` and `MTGSTOCKS RETURNED 0 RESULTS`.

**Known working examples:** Avatar: The Last Airbender, Lorwyn Eclipsed, Lorwyn Eclipsed Commander,
Teenage Mutant Ninja Turtles.

**Current hypothesis:** `EditionResolver` is functioning correctly. The issue is downstream, inside
`MtgStocksProvider`'s candidate matching / set-URL resolution against the cached MTGStocks set
index (see `ARCHITECTURE.md` → "Open Investigations" for the exact code path, confirmed against
`MtgStocksProvider.search()` and `EditionNameNormalizer.candidatesFor()`). Redis, the scheduler,
Scryfall, and historical storage are not implicated by current evidence.

**Next step (not yet started):** trace why no candidate generated by `EditionNameNormalizer` for
"Time Spiral Remastered" matches MTGStocks' actual set-index link text.

---

## This entry — Documentation Refresh

**Type:** documentation only. No production code, no tests, no commits.

**Scope:** brought `README.md` (new), `CLAUDE.md`, `PROJECT_CONTEXT.md`, `ARCHITECTURE.md`,
`PROJECT_MAP.md` (new), `DECISIONS.md` (new), and this file in line with verified current code —
re-reading every controller/service/provider/repository/entity/config class, the two manual SQL
scripts, `RedisConfig`'s cache TTLs, and re-running `./mvnw test` (51/51 passing) rather than
relying on prior summaries. Full per-file diff explanation is in the chat response delivered
alongside this log entry, not duplicated here.

**Largest finding:** `CLAUDE.md`, `PROJECT_CONTEXT.md`, `ARCHITECTURE.md`, and the old
`README.txt` all described or implied a partially-built Android/Compose mobile client. No such
code exists anywhere in this repository — only the backend exists. All four documents were
corrected to say so explicitly rather than carry that assumption forward.

---

## This entry — Final Documentation Consolidation

**Type:** documentation only. No production code, no tests, no commits.

**Scope:** consistency audit across `README.md`, `CLAUDE.md`, `PROJECT_CONTEXT.md`,
`ARCHITECTURE.md`, `DECISIONS.md`, `PROJECT_MAP.md`, and this file. Re-checked the backend-only
status, the watchlist-driven/read-only-compare scheduler flow, historical storage state, and cache
TTLs against the current code — all were already consistent across docs from the prior
Documentation Refresh pass; no contradictions found. Added a standardized "Current Project Status"
section to `README.md`, `CLAUDE.md`, and `PROJECT_CONTEXT.md` for at-a-glance findability, and
added the previously-undocumented MTGStocks set-resolution open investigation (above) to this log,
`ARCHITECTURE.md`, `README.md`, `PROJECT_CONTEXT.md`, and `PROJECT_MAP.md`.
