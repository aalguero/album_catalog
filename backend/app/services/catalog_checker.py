import asyncpg
import uuid
from typing import Optional, List, Tuple

async def find_existing_albums(
    conn: asyncpg.Connection,
    barcode: Optional[str] = None,
    catalog_number: Optional[str] = None,
    artist: Optional[str] = None,
    title: Optional[str] = None,
    discogs_release_id: Optional[int] = None,
    musicbrainz_release_id: Optional[str] = None
) -> Tuple[List[asyncpg.Record], Optional[str]]:
    """
    Search the personal vinyl_catalog for an existing album matching
    barcode, Discogs ID, MusicBrainz ID, catalog number, or artist + title.
    Returns (matching_rows, match_reason).
    """
    # 1. Barcode match (highest confidence)
    if barcode and barcode.strip():
        b = barcode.strip()
        rows = await conn.fetch(
            "SELECT * FROM albums WHERE barcode = $1 ORDER BY id DESC;",
            b
        )
        if rows:
            return rows, f"Exact barcode match ({b})"

    # 2. Discogs release ID
    if discogs_release_id:
        rows = await conn.fetch(
            "SELECT * FROM albums WHERE discogs_release_id = $1 ORDER BY id DESC;",
            discogs_release_id
        )
        if rows:
            return rows, f"Discogs release match (#{discogs_release_id})"

    # 3. MusicBrainz release ID
    if musicbrainz_release_id:
        try:
            mb_uuid = uuid.UUID(str(musicbrainz_release_id))
            rows = await conn.fetch(
                "SELECT * FROM albums WHERE musicbrainz_release_id = $1 ORDER BY id DESC;",
                mb_uuid
            )
            if rows:
                return rows, "MusicBrainz release match"
        except (ValueError, TypeError):
            pass

    # 4. Catalog number match
    if catalog_number and catalog_number.strip():
        cat = catalog_number.strip()
        clean_cat = cat.replace(" ", "").replace("-", "")
        if len(clean_cat) >= 3:
            rows = await conn.fetch(
                """
                SELECT * FROM albums 
                WHERE catalog_number IS NOT NULL AND (
                    catalog_number ILIKE $1 
                    OR replace(replace(catalog_number, ' ', ''), '-', '') ILIKE $2
                )
                ORDER BY id DESC;
                """,
                cat,
                clean_cat
            )
            if rows:
                return rows, f"Catalog number match ({cat})"

    # 5. Exact artist & title match
    if artist and title:
        a = artist.strip()
        t = title.strip()
        if a and t:
            rows = await conn.fetch(
                """
                SELECT * FROM albums 
                WHERE LOWER(TRIM(artist)) = LOWER(TRIM($1))
                  AND LOWER(TRIM(title)) = LOWER(TRIM($2))
                ORDER BY id DESC;
                """,
                a,
                t
            )
            if rows:
                return rows, f"Artist & title match ('{a}' - '{t}')"

    return [], None
