# Album Catalog - Agent Context & Operational Guide

This file is automatically loaded by Antigravity at the start of every session to persist project state, environment paths, build commands, and active priorities.

---

## 1. System Architecture & Components

- **Target Device**: Native Android app for Google Pixel 10 Pro XL.
- **Client (`/android`)**:
  - Kotlin + Jetpack Compose + CameraX + Google ML Kit (Barcode & Text Recognition).
  - UI includes:
    - Camera viewfinder with Scan Modes (Barcode, Vinyl Label with circular reticle & spindle guide).
    - Top-3 candidate ranking & Discogs pressing refinement sheets.
    - Staged Album confirmation card with Goldmine grading and Price Paid entry.
    - Collection view with search, pressing filter, manual '+' album entry, and edit sheets.
- **Backend (`/backend`)**:
  - FastAPI (Python 3.12) with `asyncpg` async connection pooling.
  - Endpoints:
    - `/api/match/scan`: Multi-tier matcher (Local MusicBrainz -> Remote MusicBrainz -> Direct Discogs API).
    - `/api/match/discogs-pressings`: Discogs release/pressing search for refinement.
    - `/api/catalog/albums`: Full CRUD (GET list/search, POST add, PUT/PATCH edit, DELETE).
- **Databases**:
  - Local PostgreSQL unix socket: `/var/run/postgresql`.
  - Database `vinyl_catalog`: Personal user catalog (`albums`, `tracks`).
  - Database `musicbrainz`: Local MusicBrainz dump for instant offline metadata queries.

---

## 2. Environment & Execution Runbook

Do NOT spend time searching for binaries or paths; use these exact configurations:

- **Java / Android SDK**:
  - `JAVA_HOME=/home/andre/developer/Android/jdk-21`
  - Android SDK: `/home/andre/Android/Sdk` and `/home/andre/developer/Android/Sdk`
  - Compile Android app:
    ```bash
    JAVA_HOME=/home/andre/developer/Android/jdk-21 ./gradlew compileDebugKotlin
    ```
    (run from `/home/andre/developer/album_catalog/android`)

- **Python Backend**:
  - Virtual Environment: `/home/andre/developer/album_catalog/backend/.venv`
  - Run Backend server:
    ```bash
    ./run_backend.sh
    # or: PYTHONPATH=backend backend/.venv/bin/python backend/app/main.py
    ```
  - Run Backend Test Suite:
    ```bash
    PYTHONPATH=backend backend/.venv/bin/python backend/tests/test_backend.py
    ```

- **Permanent Server (`futileassistant`)**:
  - Host: `futileassistant` (`100.108.86.16` Tailscale / `192.168.86.35` LAN)
  - User: `dremon`
  - Application Directory: `/home/dremon/album_catalog`
  - Container runtime: Docker Compose (`network_mode: "host"`, `restart: unless-stopped`)
  - Server commands:
    ```bash
    ssh dremon@futileassistant "cd /home/dremon/album_catalog && docker-compose ps"
    ssh dremon@futileassistant "cd /home/dremon/album_catalog && docker-compose logs -f"
    ssh dremon@futileassistant "cd /home/dremon/album_catalog && docker-compose up -d --build"
    ```
  - Databases on `futileassistant`:
    - `vinyl_catalog`: Personal user catalog (`albums`, `tracks`).
    - `musicbrainz`: Local MusicBrainz dump for instant offline metadata queries.
  - Android Base URL: `http://192.168.86.35:8000/` (LAN) or `http://100.108.86.16:8000/` (Tailscale).

---

## 3. Roadmap & Feature Status

Always check [`ROADMAP.md`](file:///home/andre/developer/album_catalog/ROADMAP.md) as the source of truth for features and defects.

### Active Phase: Phase 3 (Collection Valuation, Sync & Intelligence)
- **Completed (Phase 1: High-Throughput Scanning & Usability)**:
  1. Direct Discogs Barcode Fallback (`backend/app/routers/matcher.py`).
  2. Catalog Entry Editing & Pressing Refinement (`CatalogScreen.kt`, `EditAlbumDialog.kt`, backend PUT/PATCH).
  3. Scanning Pause & Battery Saver (unbinds CameraX sensor during dialogs and lookups).
  4. Vinyl Center Label Mode (circular reticle, spindle centroid checks, 650ms dwell timer, manual shutter).
  5. Manual Album Entry (dedicated zero-scan Manual mode pill, floating `+` FAB on collection, and blank form).
  6. Duplicate Warning & Multi-Copy Handling (`catalog_checker.py`, `StagedAlbumConfirmationCard.kt`, `Top3MatchCards.kt`).
  7. Audio & Haptic Sensory Feedback (`FeedbackManager.kt`, `ScannerViewModel.kt`, `SettingsScreen.kt`).
  8. Batch "Crate-Digging" Mode (`BatchRepository.kt`, SQLite queue, live queue badge, `BatchReviewDeckSheet.kt`).
- **Active Focus (Phase 3)**:
  1. Feature 3.1: Discogs Market Value & Pricing Tracker (`/marketplace/price_suggestions/{release_id}`).
  2. Feature 3.2: Two-Way Discogs Collection Sync & Backup/Export (CSV/JSON).
  3. Feature 3.3: Collection Insights & Analytics Dashboard.
  4. Feature 3.4: Shopping Mode (in-store acquisition & duplicate checker).
- **Long-Term Backlog (Deprioritized - Preserved in ROADMAP.md)**:
  - Dead wax / runout matrix etching OCR, Front cover visual match, Multi-disc / box sets.

---

## 4. Session Instructions
- At the start of any new session, immediately recognize this context without running exhaustive exploratory file scans.
- When adding or modifying features, update `ROADMAP.md` and this document as milestones are completed.
