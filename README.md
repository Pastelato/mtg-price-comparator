# MTG Price Comparator

A backend service that aggregates Magic: The Gathering card pricing across multiple sources, lets
a user maintain a watchlist of cards, and periodically captures historical price snapshots for
that watchlist.

> **Scope:** this repository contains the Spring Boot backend only. There is no Android/mobile
> client in this repo yet — the mobile app described under "Vision" below is planned future work,
> not current state. See `CLAUDE.md` and `PROJECT_CONTEXT.md` for the standing rules that will
> govern that work once it starts.

---

## Current Project Status

- Spring Boot backend exists and is functional.
- No Android application currently exists in this repository.
- No Jetpack Compose code currently exists.
- No Retrofit integration currently exists.
- Mobile application remains a future phase (see "Vision" below).

---

## What's implemented today

- **Price comparison** — `GET /api/cards/compare` fans out to three providers concurrently
  (Scryfall, MTGStocks, and a synthetic `MockTrader` test provider), dedupes, and returns results
  sorted by price. Read-only — it does not write any history.
- **Edition lookup** — `GET /api/cards/editions` lists known printings of a card.
- **Watchlist** — `POST/GET/DELETE /api/watchlist`. Acts as the product's current "Favorites"
  feature and is the input set for the scheduler below.
- **Scheduled historical capture** — every 5 minutes, `PriceTrackingScheduler` walks the watchlist
  and persists one row per provider result to `card_price_history`. This is the **only** writer of
  historical rows; ad-hoc searches no longer generate history.
- **Historical query** — `GET /api/cards/history` (unbounded, no pagination yet).
- **Scryfall passthrough** — search, autocomplete, and set listing, with search activity logged to
  `card_searches`.
- **Redis caching** of all outbound Scryfall/MTGStocks calls, with per-source TTLs (see below).

## Architecture

```
Client (HTTP)
    ↓
Controllers → Services → Providers (Scryfall WebClient / MTGStocks Jsoup) + Repositories (JPA)
    ↓
Redis (cache)                                                            PostgreSQL
```

```
@Scheduled, every 5 minutes
PriceTrackingScheduler → WatchlistRepository.findAll()
    → per card: CardComparisonService.compare() → HistoricalPriceService.save()
    → card_price_history
```

Full module-by-module detail: see `ARCHITECTURE.md`. Package/class index: see `PROJECT_MAP.md`.

## Tech Stack

- Java 21
- Spring Boot 3.5.14
- PostgreSQL (no Flyway/Liquibase — manual SQL under `src/main/resources/sql/`, see below)
- Redis (Spring Cache)
- Maven
- JPA / Hibernate
- Spring WebClient (Scryfall) + Jsoup 1.17.2 (MTGStocks scraping)

## Backend Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/cards/compare?name=&edition=` | Read-only price comparison |
| GET | `/api/cards/editions?name=` | Known printings of a card |
| GET | `/api/cards/history?name=` | Unbounded historical query |
| POST | `/api/watchlist?cardName=&edition=` | Idempotent add |
| GET | `/api/watchlist` | List all watched cards |
| DELETE | `/api/watchlist?cardName=` | Remove from watchlist |
| GET | `/api/cards/scryfall/search?name=` | Also logs to `card_searches` |
| GET | `/api/cards/autocomplete?q=` | Scryfall passthrough |
| GET | `/api/sets` | Scryfall passthrough |

Example `/api/cards/compare` response:

```json
[
  {
    "source": "Scryfall",
    "cardName": "Lightning Bolt",
    "edition": "Secret Lair Drop Series",
    "price": 2.97,
    "currency": "USD",
    "stock": null,
    "productUrl": "https://scryfall.com",
    "foil": null,
    "variant": null,
    "externalId": null
  }
]
```

Example `/api/cards/history` response:

```json
[
  {
    "price": 2.97,
    "provider": "Scryfall",
    "capturedAt": "2026-05-16T13:11:48"
  }
]
```

## Caching

| Cache | TTL |
|---|---|
| `cards` | 15 min |
| `cardsByEdition` | 15 min |
| `scryfallSets` | 24 h |
| `mtgstocksSetsIndex` | 24 h |
| `mtgstocksSetDetail` | 6 h |

## Database setup

`spring.jpa.hibernate.ddl-auto=update` creates tables/columns automatically, but **not**
expression-based indexes. After first run, apply these once by hand:

```bash
psql -d mtg_price_comparator -f src/main/resources/sql/card_price_history_index.sql
psql -d mtg_price_comparator -f src/main/resources/sql/watchlist_dedupe_and_unique_constraint.sql
```

Both scripts are idempotent and safe to re-run.

## Known limitations (not bugs — tracked, see `DECISIONS.md`)

- Historical writes are unconditional — no change-detection against the previous value, so real
  providers produce a high rate of duplicate-valued rows (measured 94–98%). Storage growth only
  becomes a real concern as the watchlist scales; see `AI_USAGE_LOG.md` for the investigation.
- `MockPriceProvider` is wired into the live provider list and returns a random `MockTrader` price
  on every real comparison.
- `/api/cards/history` has no pagination or date filtering.
- **Open investigation:** MTGStocks set resolution fails for some editions (e.g. Time Spiral
  Remastered) even though Scryfall resolution succeeds for the same request — root cause not yet
  found. See `ARCHITECTURE.md` ("Open Investigations") and `AI_USAGE_LOG.md`.

## Development Setup

```bash
./mvnw spring-boot:run
```

Backend URL: `http://localhost:8080`

## Testing

```bash
./mvnw test
```

51 tests across 8 test classes (`CardComparisonServiceTest`, `EditionResolverTest`,
`EditionNameNormalizerTest`, `MtgStocksProviderTest`, `PriceTrackingSchedulerTest`,
`WatchlistServiceTest`, `WatchlistControllerTest`, `MtgPriceComparatorApplicationTests`) —
all passing as of this writing.

---

## Vision (mobile app — planned, not implemented)

The long-term product is a mobile market-intelligence app combining price comparison, historical
trends, collection tracking, portfolio management, and market analytics — inspired by TradingView,
Delta Investment Tracker, MTGGoldfish, Moxfield, and Cardmarket. No Android/Kotlin/Compose code
exists in this repository yet; see `CLAUDE.md` for the architecture/UI rules that will apply once
that work begins.

### Roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 | Search, price comparison | Backend done; mobile UI not started |
| 2 | Historical charts | Backend data available; no charting UI |
| 3 | Favorites | Backend equivalent (watchlist) done; no mobile UI |
| 4 | Portfolio | Not started |
| 5 | Price alerts | Not started |
| 6 | AI-powered insights | Not started |

## License

This project is intended for educational, portfolio, and personal use.
