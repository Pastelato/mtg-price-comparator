# PROJECT_CONTEXT.md

# MTG Price Comparator

## Overview

MTG Price Comparator aggregates Magic: The Gathering card pricing across providers, tracks
historical price movements for a user-curated watchlist, and is intended to eventually back a
mobile market-intelligence app.

**This repository currently contains the backend only.** There is no Android/Kotlin/Compose
module on disk. References to a mobile client below describe planned, not-yet-built work.

## Current Project Status

- Spring Boot backend exists and is functional.
- No Android application currently exists in this repository.
- No Jetpack Compose code currently exists.
- No Retrofit integration currently exists.
- Mobile application remains a future phase (see "Product Vision" below).

### Backend (implemented, this repository)

Spring Boot REST API. Responsibilities:

- Card search and multi-provider price comparison (read-only)
- Edition resolution and listing
- Watchlist management (add/list/delete)
- Scheduled historical price capture for watchlisted cards
- Historical price query
- Redis caching of upstream provider calls
- PostgreSQL persistence

### Mobile (planned — not present in this repository)

Android application intended to be built with Kotlin and Jetpack Compose. No code for this exists
yet. See `CLAUDE.md` for the standing architecture/UI rules that will apply once it starts.

---

# Current Architecture

```
Client (HTTP)
    ↓
Spring Boot API
    ↓
PostgreSQL  /  Redis
```

A separate scheduled job (`PriceTrackingScheduler`, every 5 minutes) reads the watchlist and is the
only writer of historical price rows. See `ARCHITECTURE.md` for the full data/control flow.

---

# Backend Stack

- Java 21
- Spring Boot 3.5.14
- Maven
- PostgreSQL (no Flyway/Liquibase — manual SQL scripts under `src/main/resources/sql/`)
- Redis (Spring Cache + `spring-boot-starter-data-redis`)
- JPA / Hibernate
- Spring WebClient (Scryfall API client)
- Jsoup 1.17.2 (MTGStocks scraping)
- Scryfall API + MTGStocks (scraped) as external pricing sources

---

# Android Stack (planned, not yet started)

- Kotlin
- Jetpack Compose
- Material 3
- Retrofit
- MVVM
- Navigation Compose

---

# Existing Backend Endpoints

## Compare Card Prices (read-only)

```
GET /api/cards/compare?name=&edition=
```

Aggregates results from every registered `CardPriceProvider` (Scryfall, MTGStocks, and a synthetic
`MockTrader` provider that is currently always active — see `ARCHITECTURE.md`), deduplicates, and
sorts ascending by price. Does **not** write to `card_price_history`.

Example response:

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

## Editions

```
GET /api/cards/editions?name=
```

Lists known printings of a card (set name, set code, collector number, image URL).

## Historical Prices

```
GET /api/cards/history?name=
```

Unbounded — no pagination or date filtering. Returns rows written only by the scheduler.

Example response:

```json
[
  {
    "price": 2.97,
    "provider": "Scryfall",
    "capturedAt": "2026-05-16T13:11:48"
  }
]
```

## Watchlist

```
POST   /api/watchlist?cardName=&edition=
GET    /api/watchlist
DELETE /api/watchlist?cardName=
```

Adding is idempotent (existence check + unique-constraint-safe insert). This is the input set the
scheduler walks every 5 minutes.

## Scryfall passthrough

```
GET /api/cards/scryfall/search?name=   (also records a row in card_searches)
GET /api/cards/autocomplete?q=
GET /api/sets
```

---

# Existing Models

## CardPriceResult

Fields:

- source
- cardName
- edition
- price
- currency
- stock
- productUrl
- foil
- variant
- externalId

## HistoricalPriceDto

Fields:

- price
- provider
- capturedAt

## EditionDto

Fields:

- setName
- setCode
- collectorNumber
- imageUrl

---

# Retrofit Configuration (planned, mobile side — not present in this repo)

Base URL (intended):

```
http://10.0.2.2:8080
```

`10.0.2.2` is required (not `localhost`) when targeting an Android Emulator from a future mobile
client.

---

# Current Features

## Implemented (backend, this repository)

- Card search backend (Scryfall + MTGStocks + Mock providers)
- Read-only price comparison and dedupe
- Edition resolution and listing
- Watchlist (functions as the "favorites" mechanism today)
- Scheduled historical price capture, driven by the watchlist
- Historical price query (unbounded)
- Redis caching with per-source TTLs
- PostgreSQL persistence (manual index scripts, no migration tool)

## Not implemented (no code exists for these)

- Mobile app (Android/Compose/Retrofit) — zero code in this repo
- Portfolio tracking
- Price alerts / push notifications
- Pagination or date-range filtering on `/api/cards/history`
- Change-detection before historical writes (writes are currently unconditional — see Sprint 5B findings in `AI_USAGE_LOG.md`)
- Retention policy for `card_price_history`

## Known Issues

- **Open investigation:** MTGStocks set resolution fails for some editions (e.g. Time Spiral
  Remastered) even though Scryfall edition resolution succeeds for the same request. Root cause
  not yet found — see `ARCHITECTURE.md` ("Open Investigations") and `AI_USAGE_LOG.md`.

---

# Product Vision

Create the best mobile application for MTG card market tracking, combining:

- Price comparison
- Historical trends
- Collection tracking
- Portfolio management
- Market analytics

Inspired by:

- TradingView
- Delta Investment Tracker
- Moxfield
- MTGGoldfish
- Cardmarket

This remains the long-term target; the backend pieces built so far (search, compare, watchlist,
history) are the foundation for it, not the finished product.
