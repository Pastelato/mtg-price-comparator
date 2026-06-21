# PROJECT_MAP.md

Package-by-package index of `src/main/java/com/smichelotti/mtg`. For behavior and data flow, see
`ARCHITECTURE.md`. This file is a navigation aid only — it lists what exists and a one-line
purpose, not how the pieces interact.

---

## `controller`

| Class | Purpose |
|---|---|
| `CardComparisonController` | `/api/cards/compare`, `/api/cards/editions` |
| `HistoricalPriceController` | `/api/cards/history` |
| `WatchlistController` | `/api/watchlist` (POST/GET/DELETE) |
| `ScryfallController` | `/api/cards/scryfall/search`, `/api/cards/autocomplete`, `/api/sets` |
| `TestController` | `/api/test` — dev scaffolding, not part of the product surface |

## `service`

| Class | Purpose |
|---|---|
| `CardComparisonService` | Read-only multi-provider aggregation, dedupe, sort |
| `EditionResolver` | Raw edition string → Scryfall set code/name |
| `CardEditionService` | Lists known printings of a card |
| `HistoricalPriceService` | Writes `card_price_history` (scheduler-only caller) |
| `HistoricalQueryService` | Reads `card_price_history` |
| `CardSearchPersistenceService` | Writes `card_searches` (Scryfall search endpoint only) |
| `WatchlistService` | Add/delete watchlist rows, idempotent add |
| `PriceTrackingScheduler` | `@Scheduled` 5-min job; sole writer of historical rows |

## `provider`

| Class | Purpose |
|---|---|
| `CardPriceProvider` | Interface implemented by every price source |
| `ScryfallPriceProvider` | Scryfall-backed provider |
| `MtgStocksProvider` | MTGStocks-backed provider (matching/parsing logic). Candidate-matching / set-URL resolution here is the subject of an open investigation — see `AI_USAGE_LOG.md` |
| `MtgStocksScraper` | Jsoup HTTP calls for MTGStocks, separated out so `@Cacheable` applies |
| `MockPriceProvider` | Synthetic random-price provider, always active |
| `EditionNameNormalizer` | Reconciles Scryfall vs. MTGStocks set-name conventions. Candidate generation here is a leading suspect in the open MTGStocks set-resolution investigation |

## `client`

| Class | Purpose |
|---|---|
| `ScryfallClient` | `WebClient`-based wrapper around the Scryfall API, with `@Cacheable` methods |

## `repository`

| Class | Purpose |
|---|---|
| `WatchlistRepository` | JPA repository for `watchlist` |
| `CardPriceHistoryRepository` | JPA repository for `card_price_history` |
| `CardSearchRepository` | JPA repository for `card_searches` |

## `entity`

| Class | Table |
|---|---|
| `WatchlistEntity` | `watchlist` |
| `CardPriceHistoryEntity` | `card_price_history` |
| `CardSearchEntity` | `card_searches` |

## `dto`

| Class | Purpose |
|---|---|
| `CardPriceResult` | Provider search result (`source`, `cardName`, `edition`, `price`, `currency`, `stock`, `productUrl`, `foil`, `variant`, `externalId`) |
| `HistoricalPriceDto` | History query result (`price`, `provider`, `capturedAt`) |
| `EditionDto` | Edition listing result (`setName`, `setCode`, `collectorNumber`, `imageUrl`) |
| `ResolvedEdition` | `record(original, setCode, setName)` |
| `ScryfallCardResponse`, `ScryfallSearchResponse`, `ScryfallSetsResponse`, `ScryfallSetDto`, `ScryfallAutocompleteResponse`, `ScryfallPricesDto`, `ImageUris` | Scryfall API response shapes |
| `MtgStocksSetLinkDto`, `MtgStocksPrintDto` | MTGStocks scrape result shapes |

## `config`

| Class | Purpose |
|---|---|
| `RedisConfig` | `RedisCacheManager` bean, per-cache TTLs, scoped polymorphic Jackson typing |
| `MtgStocksProperties` | `@ConfigurationProperties(prefix = "mtgstocks")` — base URL, paths, per-request timeouts/user-agents, debug flags |

## `scraper`

| Class | Purpose |
|---|---|
| `JsoupTest` | Standalone `main()` dev utility scraping a hardcoded Card Kingdom URL. Not wired into Spring; lives in `src/main` despite being a manual test tool — flagged as cleanup candidate in `ARCHITECTURE.md`. |

## Top level

| Class | Purpose |
|---|---|
| `MtgPriceComparatorApplication` | `@SpringBootApplication`, `@EnableCaching`, `@EnableScheduling`, `@ConfigurationPropertiesScan` |

## `src/main/resources`

| File | Purpose |
|---|---|
| `application.properties` | Spring/datasource/Redis/MTGStocks config |
| `sql/card_price_history_index.sql` | Manual index on `UPPER(card_name), captured_at` |
| `sql/watchlist_dedupe_and_unique_constraint.sql` | One-time dedupe + unique index on `(card_name, COALESCE(edition,''))` |

## `src/test/java/com/smichelotti/mtg`

| File | Covers |
|---|---|
| `MtgPriceComparatorApplicationTests` | Context load |
| `service/CardComparisonServiceTest` | Aggregation/dedupe/sort |
| `service/EditionResolverTest` | Edition resolution |
| `provider/EditionNameNormalizerTest` | Set-name candidate generation |
| `provider/MtgStocksProviderTest` | MTGStocks matching logic |
| `service/PriceTrackingSchedulerTest` | Scheduler iteration + failure isolation |
| `service/WatchlistServiceTest` | Add/delete + race-condition handling |
| `controller/WatchlistControllerTest` | Watchlist REST surface |

51 tests total across these 8 classes.
