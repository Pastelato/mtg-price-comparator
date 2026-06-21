# CLAUDE.md

# MTG Price Comparator Development Rules

## Read First

Always read:

- PROJECT_CONTEXT.md
- ARCHITECTURE.md

before generating code.

---

# Current Implementation Status

This repository contains the **Spring Boot backend only** — no Android/Kotlin/Compose code exists
on disk yet. The Architecture/UI/Design rules below are standing rules for that mobile client once
work on it begins; until then, there is nothing in this repo for them to apply to.

Backend-side, the watchlist feature (`/api/watchlist`) is the current equivalent of "Favorites".
`GET /api/cards/compare` is read-only: it does not persist anything. Only the scheduler
(`PriceTrackingScheduler`, every 5 minutes, driven by the watchlist) writes to `card_price_history`.
See `ARCHITECTURE.md` for the verified, current data/control flow.

Current Project Status, explicitly:

- Spring Boot backend exists and is functional.
- No Android application currently exists in this repository.
- No Jetpack Compose code currently exists.
- No Retrofit integration currently exists — the "Architecture Rules" / "Networking Rules"
  sections below are standing rules for when that work starts, not a description of code on disk
  today.
- Mobile application remains a future phase.

---

# Architecture Rules

Use MVVM architecture.

Never place business logic inside Composable functions.

Always follow:

UI
→ ViewModel
→ Repository
→ Retrofit
→ Backend API

---

# UI Rules

Use:

- Material 3
- Jetpack Compose
- Dark Theme

Prefer reusable composables.

Avoid duplicated code.

---

# Code Generation Rules

When generating code:

1. Show complete file path.
2. Show package declaration.
3. Show imports.
4. Generate complete code.
5. Explain where files must be created.

Never generate partial snippets.

---

# Networking Rules

Use existing Retrofit configuration.

Do not create new API endpoints unless explicitly requested.

Do not change backend contracts.

Use existing models whenever possible.

---

# Existing Models

CardPriceResult

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

HistoricalPriceDto

Fields:

- price
- provider
- capturedAt

EditionDto

Fields:

- setName
- setCode
- collectorNumber
- imageUrl

---

# Existing Backend Endpoints

GET /api/cards/compare?name=&edition=   (read-only, aggregates all providers)

GET /api/cards/editions?name=

GET /api/cards/history?name=   (unbounded — no pagination/date range yet)

POST /api/watchlist?cardName=&edition=

GET /api/watchlist

DELETE /api/watchlist?cardName=

GET /api/cards/scryfall/search?name=

GET /api/cards/autocomplete?q=

GET /api/sets

---

# Design Guidelines

The application should look modern and premium.

Visual inspiration:

- TradingView
- Discord
- Linear
- Delta
- Material You

Colors:

- Dark background
- Purple accent color
- Green for positive prices
- Red for negative prices

---

# Current Milestone

Current goal:

Create a professional mobile application that displays:

- Search
- Historical Charts
- Favorites
- Portfolio

using the existing backend APIs.

Status: the backend already exposes Search (`/api/cards/compare`, `/api/cards/editions`),
Historical Charts data (`/api/cards/history`), and a Favorites equivalent (`/api/watchlist`).
Portfolio has no backend support yet. No mobile UI has been built for any of these.

Do not redesign architecture without justification.
