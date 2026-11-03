# Album Catalog

A native Android app + FastAPI backend for rapidly cataloging a physical vinyl record collection by scanning barcodes and vinyl labels, matching against MusicBrainz and Discogs, and tracking collection value over time.

Built to replace tedious manual data entry across a collection of thousands of records — the core design goal is to minimize photos taken and never require pulling the vinyl out of its sleeve.

## How it works

1. Point the Android app's camera at a barcode, vinyl center label, or spine.
2. The backend resolves it through a multi-tier matcher: local MusicBrainz dump → remote MusicBrainz → direct Discogs API.
3. Top-3 candidate matches are ranked and presented for the user to confirm, with Discogs pressing refinement for ambiguous releases.
4. The confirmed album (with Goldmine condition grade, price paid, shelf location) is saved to the catalog.

## Architecture

- **`/android`** — Kotlin + Jetpack Compose + CameraX + Google ML Kit (barcode & text recognition). Targets a Pixel 10 Pro XL.
  - Scanner with Barcode / Vinyl Label / Manual entry modes
  - Batch "crate-digging" mode with an offline SQLite queue and swipeable review deck
  - Staged confirmation card, duplicate/multi-copy detection, collection browser with search and edit
- **`/backend`** — FastAPI (Python 3.12), `asyncpg` connection pooling
  - `/api/match/scan` — multi-tier barcode/label matcher
  - `/api/match/discogs-pressings` — Discogs release/pressing refinement search
  - `/api/catalog/albums` — full CRUD for the personal catalog
- **PostgreSQL**
  - `vinyl_catalog` — the personal collection (`albums`, `tracks`)
  - `musicbrainz` — a full local MusicBrainz dump for instant offline metadata lookups, avoiding network round-trips on every scan

## Status

Project is actively developed and in daily use for cataloging a real collection. See [`ROADMAP.md`](./ROADMAP.md) for full feature specs and defect history.

- **Phase 1 — High-Throughput Scanning & Usability: Complete.** Batch mode, manual entry, duplicate handling, audio/haptic feedback, vinyl label scanning, battery-aware camera handling.
- **Phase 3 — Collection Valuation, Sync & Intelligence: Active.** Discogs market value tracking, two-way Discogs sync, CSV/JSON export, analytics dashboard, in-store shopping mode.
- **Long-term backlog (deprioritized):** dead-wax/runout matrix OCR, cover-art visual matching, multi-disc/box-set support.

## Running it

See [`AGENTS.md`](./AGENTS.md) for full environment paths, build commands, and the permanent server runbook. Quick start:

```bash
# Backend (local)
./run_backend.sh

# Backend (Docker)
cd backend && docker-compose up -d --build

# Backend tests
PYTHONPATH=backend backend/.venv/bin/python backend/tests/test_backend.py

# Android (debug build)
cd android && JAVA_HOME=/path/to/jdk-21 ./gradlew compileDebugKotlin
```

The backend expects a local PostgreSQL instance with `vinyl_catalog` and `musicbrainz` databases; see `backend/.env.example` for required configuration.

## Tech stack

- **Client:** Kotlin, Jetpack Compose, CameraX, ML Kit
- **Backend:** FastAPI, asyncpg, httpx, rapidfuzz
- **Data:** PostgreSQL (personal catalog + local MusicBrainz mirror), Discogs API
- **Deploy:** Docker Compose on a dedicated home server
