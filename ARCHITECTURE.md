# ARCHITECTURE.md

# MTG Price Comparator — Backend Architecture

> Scope note: this repository contains the **Spring Boot backend only**. No Android/Kotlin/Compose
> module exists on disk anywhere in this repo. The "Android Layers" section near the bottom of
> this document describes a **planned, not-yet-built** mobile client — see `CLAUDE.md` for the
> standing rules that will govern it once that work starts.

---

## High-Level Architecture (as implemented)

```
Client (HTTP)
    ↓
Controllers (REST)
    ↓
Services (business logic)
    ↓
Providers (Scryfall WebClient / MTGStocks Jsoup scraper)  +  Repositories (JPA)
    ↓
Redis (cache)                                                 PostgreSQL
```

A second, independent flow runs on a timer and is the **only** path that writes to
`card_price_history`:

```
@Scheduled (every 5 minutes)
PriceTrackingScheduler
    ↓
WatchlistRepository.findAll()
    ↓ (per watched card)
CardComparisonService.compare()  →  HistoricalPriceService.save()
    ↓
card_price_history
```

`GET /api/cards/compare` itself is **read-only**: `CardComparisonService.compare()` aggregates,
deduplicates, and sorts provider results but does not persist anything. User-triggered searches no
longer generate historical rows — only the scheduler does, and only for cards on the watchlist.

---

# Backend Modules

## Controllers

| Controller | Endpoints | Notes |
|---|---|---|
| `CardComparisonController` | `GET /api/cards/compare?name=&edition=`, `GET /api/cards/editions?name=` | `/compare` is read-only |
| `HistoricalPriceController` | `GET /api/cards/history?name=` | Unbounded — no pagination or date range |
| `WatchlistController` | `POST /api/watchlist?cardName=&edition=`, `GET /api/watchlist`, `DELETE /api/watchlist?cardName=` | Backs the scheduler's input set |
| `ScryfallController` | `GET /api/cards/scryfall/search?name=`, `GET /api/cards/autocomplete?q=`, `GET /api/sets` | `/scryfall/search` is the only endpoint that writes to `card_searches`; has no logger (gap, not yet remediated) |
| `TestController` | `GET /api/test` | Dev scaffolding left over from initial setup — hardcoded response, not part of the product surface |

## Services

| Service | Responsibility |
|---|---|
| `CardComparisonService` | Fans out to all `CardPriceProvider` beans concurrently, dedupes by `(source, externalId)` or `(source, cardName, edition, foil)`, sorts by price. **Read-only** — does not persist. |
| `EditionResolver` | Resolves a raw edition string (code or name) to a Scryfall set code/name via `ScryfallClient.getSets()`. Passes through unresolved input if no match is found. |
| `CardEditionService` | Lists all known printings/editions of a card name (backs `GET /api/cards/editions`). |
| `HistoricalPriceService` | Writes one row to `card_price_history` per `CardPriceResult`. Called **only** by `PriceTrackingScheduler`. |
| `HistoricalQueryService` | Reads `card_price_history` (backs `GET /api/cards/history`). |
| `CardSearchPersistenceService` | Writes one row to `card_searches`. Called **only** by `ScryfallController.search()`. |
| `WatchlistService` | Add/delete watchlist rows. `addCard` is idempotent: an existence check is followed by a try/save that treats a `DataIntegrityViolationException` (lost race against the unique index) as a no-op. |
| `PriceTrackingScheduler` | `@Scheduled(fixedRate = 300000)` (5 min). Iterates `WatchlistRepository.findAll()`, calls `CardComparisonService.compare()` per card, and is the sole caller of `HistoricalPriceService.save()`. A failure on one card (caught and logged at ERROR) does not stop the remaining cards in the cycle. |

**Known inconsistency:** `CardComparisonService` still has `HistoricalPriceService` injected as a field but never calls it — a leftover from before the scheduler redesign made `compare()` read-only. Functionally harmless (Spring just wires an unused dependency); flagged here as cleanup for a future sprint, not fixed in this pass since this is a documentation-only change.

## Providers

`CardPriceProvider` is the common interface (`search(cardName, ResolvedEdition)`, `getSourceName()`). Three implementations are registered as Spring beans and all run on every `compare()` call:

- **`ScryfallPriceProvider`** — calls `ScryfallClient` (Spring `WebClient`). Returns Scryfall's USD price.
- **`MtgStocksProvider`** + **`MtgStocksScraper`** — scrapes mtgstocks.com via Jsoup. Split into two beans specifically so `@Cacheable` on the scraper's methods is honored (Spring's caching proxy doesn't intercept self-invocation, so the caching had to be moved out of `MtgStocksProvider` itself). `EditionNameNormalizer` bridges naming differences between Scryfall's and MTGStocks' set names (e.g. `"<Set> Commander"` vs `"Commander: <Set>"`).
- **`MockPriceProvider`** — generates a random price (`$2.00–$5.00`) under source `"MockTrader"`. **Still wired into the live provider list** — every real `/api/cards/compare` call includes a synthetic MockTrader result alongside real providers. (This was the subject of the Sprint 5A investigation — see `AI_USAGE_LOG.md`.)

## Persistence (PostgreSQL)

| Table | Entity | Written by | Fields |
|---|---|---|---|
| `watchlist` | `WatchlistEntity` | `WatchlistService` | `id`, `card_name`, `edition`, `created_at` |
| `card_price_history` | `CardPriceHistoryEntity` | `HistoricalPriceService` (scheduler only) | `id`, `card_name`, `edition`, `provider`, `price`, `currency`, `captured_at` |
| `card_searches` | `CardSearchEntity` | `CardSearchPersistenceService` (Scryfall search endpoint only) | `id`, `card_name`, `edition`, `searched_at` |

There is **no Flyway/Liquibase**. `spring.jpa.hibernate.ddl-auto=update` creates tables/columns from
`@Entity` mappings only — it never creates indexes beyond what JPA annotations declare. Two manual,
idempotent SQL scripts under `src/main/resources/sql/` must be run by hand against the target
database:

- **`card_price_history_index.sql`** — `CREATE INDEX ... ON card_price_history (UPPER(card_name), captured_at)`. Required because `findByCardNameIgnoreCaseOrderByCapturedAtAsc` compiles to an `UPPER(card_name) = UPPER(?)` predicate (confirmed against live `show-sql` output, not assumed) — a plain B-tree index on the raw column would not be used by the planner.
- **`watchlist_dedupe_and_unique_constraint.sql`** — one-time cleanup of pre-existing duplicate watchlist rows, then `CREATE UNIQUE INDEX ON watchlist (card_name, COALESCE(edition, ''))`, since Postgres treats `NULL <> NULL` in a plain unique constraint and would not catch a `(card_name, NULL)` duplicate otherwise.

### Historical write behavior (Sprint 5B finding)

Every scheduler tick performs an **unconditional write** per provider result — there is no
change-detection against the most recent row before `save()`. Real providers (Scryfall, MTGStocks)
were measured to produce 94–98% duplicate-value rows across consecutive ticks. This is accepted,
known behavior, not a bug; see `AI_USAGE_LOG.md` and `DECISIONS.md` for the investigation and the
recommended (not yet implemented) follow-up: change detection before `save()`, pagination/date
ranges on `/api/cards/history`, and a retention policy.

## Caching (Redis)

Configured in `RedisConfig` — one `RedisCacheManager` with per-cache TTL overrides, default 10 min:

| Cache name | TTL | Populated by |
|---|---|---|
| `cards` | 15 min | `ScryfallClient.searchCard` |
| `cardsByEdition` | 15 min | `ScryfallClient.searchCardByEdition` |
| `scryfallSets` | 24 h | `ScryfallClient.getSets` |
| `mtgstocksSetsIndex` | 24 h | `MtgStocksScraper.fetchSetsIndex` |
| `mtgstocksSetDetail` | 6 h | `MtgStocksScraper.fetchSetDetail` |

Cache values are serialized with `GenericJackson2JsonRedisSerializer`, scoped to a polymorphic type
validator that only allows `com.smichelotti.mtg.dto`, `java.util.*`, and `java.lang.*` subtypes —
narrowed from an earlier `allowIfSubType(Object.class)` configuration during a security review
referenced in code as "Fase 6" (see `DECISIONS.md`).

---

# Logging

SLF4J (`org.slf4j.Logger`) is the sole logging mechanism — there is no `System.out.println` or
`printStackTrace` anywhere in `src`. Levels follow: INFO for business-significant events
(compare/resolve requests, watchlist add/delete, scheduler cycle start, per-request result counts),
DEBUG for diagnostic/trace detail (outbound call markers, candidate lists, per-print matching),
WARN for unexpected-but-recoverable conditions (edition not found, lost insert race, null
price/card guards), ERROR for caught exceptions in catch blocks (always logged with the exception
object, e.g. `log.error("...", e)`). See `AI_USAGE_LOG.md` (Sprint 1) for the cleanup pass that
established this and `application.properties` for the disabled `spring.jpa.show-sql` /
`format_sql` overrides (production-safe default: off).

---

# Open Investigations

## MTGStocks set resolution failures (status: open, not yet root-caused)

Some cards return zero MTGStocks results even though `EditionResolver` correctly resolves their
Scryfall set code/name. Observed failing example: **Time Spiral Remastered** (`setCode=tsr`) —
`MtgStocksProvider` logs `SET URL NOT FOUND` / `MTGSTOCKS RETURNED 0 RESULTS`. Observed working
examples: Avatar: The Last Airbender, Lorwyn Eclipsed, Lorwyn Eclipsed Commander, Teenage Mutant
Ninja Turtles.

**Where this happens in code** (`MtgStocksProvider.search()`): `EditionNameNormalizer.candidatesFor()`
generates a small set of candidate strings (original name, Commander/Art Series prefix swap,
trailing-qualifier removal, code-suffix variant), and each candidate is compared via
`equalsIgnoreCase` against the link text returned by `scraper.fetchSetsIndex()`. If none match
exactly, `setUrl` stays `null` and the provider returns an empty result — the `SET URL NOT FOUND`
branch.

**Current hypothesis:** `EditionResolver` is functioning correctly (it resolves the Scryfall side
without issue). The failure is downstream, in one of:

- `EditionNameNormalizer` candidate generation — no existing strategy accounts for whatever naming
  difference MTGStocks uses for "Remastered"-style sets.
- The exact-match comparison against `scraper.fetchSetsIndex()` link text.
- The cached `mtgstocksSetsIndex` content itself (24h TTL).

**Not implicated by current evidence** — and therefore not where the next investigation should
start: Redis, the scheduler, Scryfall, or `card_price_history`. Start in `MtgStocksProvider`'s
candidate-matching path described above. See `AI_USAGE_LOG.md` for the investigation log entry.

---

# Mobile App (planned — not implemented in this repository)

No Kotlin/Android/Compose code exists in this repo. The layers below describe the intended future
client, per `CLAUDE.md`'s standing architecture rules (MVVM, UI → ViewModel → Repository →
Retrofit → Backend API). They are forward-looking design intent, not current state.

## UI

Jetpack Compose Screens (planned): SearchScreen, HistoryScreen, FavoritesScreen, PortfolioScreen, SettingsScreen.

## ViewModels

Planned: SearchViewModel, HistoryViewModel, and others mirroring each screen above.

## Repository (Android-side data layer)

Planned: a Retrofit-backed `CardRepository` wrapping the backend endpoints listed above.

## Networking

Retrofit, targeting the endpoints documented under "Controllers" above (not the abbreviated
two-endpoint list in earlier drafts of this document).

---

# Future Roadmap

Phase 1 — Search, price comparison *(backend implemented; mobile UI not started)*

Phase 2 — Historical charts *(backend data available via `/api/cards/history`; no charting UI exists)*

Phase 3 — Favorites *(backend equivalent exists today as the watchlist feature; no mobile UI exists)*

Phase 4 — Portfolio *(not started; no backend or mobile code)*

Phase 5 — Alerts *(not started)*

Phase 6 — AI-powered market insights *(not started)*
