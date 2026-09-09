import re
from typing import List, Optional, Dict, Any
from app.models.matcher import MatchCandidate, TrackItem

def format_duration(ms: Optional[int]) -> Optional[str]:
    if not ms or ms <= 0:
        return None
    seconds = int(ms / 1000)
    minutes = seconds // 60
    rem_seconds = seconds % 60
    return f"{minutes}:{rem_seconds:02d}"

def clean_code(code: Optional[str]) -> str:
    if not code:
        return ""
    return re.sub(r'[^a-zA-Z0-9]', '', code).strip()

async def get_tracks_for_release(conn, release_id: int) -> List[TrackItem]:
    query = """
    SELECT 
        t.number AS position,
        t.name AS title,
        t.length AS duration_ms
    FROM musicbrainz.medium m
    JOIN musicbrainz.track t ON t.medium = m.id
    WHERE m.release = $1
    ORDER BY m.position, t.position;
    """
    rows = await conn.fetch(query, release_id)
    tracks = []
    for r in rows:
        tracks.append(TrackItem(
            position=r["position"],
            title=r["title"],
            duration=format_duration(r["duration_ms"])
        ))
    return tracks

def row_to_candidate(row, tracks: List[TrackItem], confidence: int, reason: str) -> MatchCandidate:
    gid_str = str(row["gid"])
    cover_url = f"https://coverartarchive.org/release/{gid_str}/front-500"
    thumb_url = f"https://coverartarchive.org/release/{gid_str}/front-250"
    
    return MatchCandidate(
        title=row["title"],
        artist=row["artist"] or "Unknown Artist",
        release_year=row["release_year"],
        label=row["label"],
        catalog_number=row["catalog_number"],
        barcode=row["barcode"],
        format=row["format_name"] or "Vinyl",
        country=row["country"],
        musicbrainz_release_id=gid_str,
        cover_image_url=cover_url,
        thumbnail_url=thumb_url,
        tracks=tracks,
        source="local_musicbrainz",
        confidence_score=confidence,
        match_reason=reason
    )

async def search_by_barcode(conn, barcode: str) -> List[MatchCandidate]:
    raw_barcode = barcode.strip()
    cleaned = clean_code(raw_barcode)
    # Search for both exact and padded/unpadded forms
    candidates_barcodes = list(set([raw_barcode, cleaned, cleaned.lstrip('0')]))
    
    query = """
    SELECT DISTINCT ON (r.id)
        r.id,
        r.gid,
        r.name AS title,
        r.barcode,
        ac.name AS artist,
        rc.date_year AS release_year,
        a.name AS country,
        l.name AS label,
        rl.catalog_number,
        mf.name AS format_name
    FROM musicbrainz.release r
    JOIN musicbrainz.artist_credit ac ON r.artist_credit = ac.id
    LEFT JOIN musicbrainz.release_label rl ON rl.release = r.id
    LEFT JOIN musicbrainz.label l ON rl.label = l.id
    LEFT JOIN musicbrainz.release_country rc ON rc.release = r.id
    LEFT JOIN musicbrainz.area a ON rc.country = a.id
    LEFT JOIN musicbrainz.medium m ON m.release = r.id
    LEFT JOIN musicbrainz.medium_format mf ON m.format = mf.id
    WHERE r.barcode = ANY($1::text[])
    ORDER BY r.id, (CASE WHEN mf.name ILIKE '%vinyl%' THEN 0 ELSE 1 END)
    LIMIT 5;
    """
    rows = await conn.fetch(query, candidates_barcodes)
    results = []
    for r in rows:
        tracks = await get_tracks_for_release(conn, r["id"])
        is_vinyl = "vinyl" in (r["format_name"] or "").lower()
        confidence = 98 if is_vinyl else 92
        reason = f"Exact barcode match ({r['barcode']})" + (" [Vinyl format]" if is_vinyl else "")
        results.append(row_to_candidate(r, tracks, confidence, reason))
    return results

async def search_by_catalog_number(conn, cat_no: str, artist_hint: Optional[str] = None) -> List[MatchCandidate]:
    cleaned = clean_code(cat_no)
    if not cleaned:
        return []

    query = """
    SELECT DISTINCT ON (r.id)
        r.id,
        r.gid,
        r.name AS title,
        r.barcode,
        ac.name AS artist,
        rc.date_year AS release_year,
        a.name AS country,
        l.name AS label,
        rl.catalog_number,
        mf.name AS format_name
    FROM musicbrainz.release_label rl
    JOIN musicbrainz.release r ON rl.release = r.id
    JOIN musicbrainz.artist_credit ac ON r.artist_credit = ac.id
    LEFT JOIN musicbrainz.label l ON rl.label = l.id
    LEFT JOIN musicbrainz.release_country rc ON rc.release = r.id
    LEFT JOIN musicbrainz.area a ON rc.country = a.id
    LEFT JOIN musicbrainz.medium m ON m.release = r.id
    LEFT JOIN musicbrainz.medium_format mf ON m.format = mf.id
    WHERE rl.catalog_number = $1 OR UPPER(REPLACE(rl.catalog_number, ' ', '')) = UPPER($2)
    ORDER BY r.id, (CASE WHEN mf.name ILIKE '%vinyl%' THEN 0 ELSE 1 END)
    LIMIT 5;
    """
    rows = await conn.fetch(query, cat_no.strip(), cleaned)
    results = []
    for r in rows:
        tracks = await get_tracks_for_release(conn, r["id"])
        is_vinyl = "vinyl" in (r["format_name"] or "").lower()
        confidence = 88 if is_vinyl else 75
        reason = f"Catalog number match ({r['catalog_number']})"
        results.append(row_to_candidate(r, tracks, confidence, reason))
    return results

async def search_by_artist_title(conn, artist: str, title: str) -> List[MatchCandidate]:
    artist_pat = f"%{artist.strip()}%"
    title_pat = f"%{title.strip()}%"
    
    query = """
    SELECT DISTINCT ON (r.id)
        r.id,
        r.gid,
        r.name AS title,
        r.barcode,
        ac.name AS artist,
        rc.date_year AS release_year,
        a.name AS country,
        l.name AS label,
        rl.catalog_number,
        mf.name AS format_name
    FROM musicbrainz.release r
    JOIN musicbrainz.artist_credit ac ON r.artist_credit = ac.id
    LEFT JOIN musicbrainz.release_label rl ON rl.release = r.id
    LEFT JOIN musicbrainz.label l ON rl.label = l.id
    LEFT JOIN musicbrainz.release_country rc ON rc.release = r.id
    LEFT JOIN musicbrainz.area a ON rc.country = a.id
    LEFT JOIN musicbrainz.medium m ON m.release = r.id
    LEFT JOIN musicbrainz.medium_format mf ON m.format = mf.id
    WHERE r.name ILIKE $1 AND ac.name ILIKE $2
    ORDER BY r.id, (CASE WHEN mf.name ILIKE '%vinyl%' THEN 0 ELSE 1 END)
    LIMIT 5;
    """
    rows = await conn.fetch(query, title_pat, artist_pat)
    results = []
    for r in rows:
        tracks = await get_tracks_for_release(conn, r["id"])
        is_vinyl = "vinyl" in (r["format_name"] or "").lower()
        confidence = 80 if is_vinyl else 65
        reason = f"Title and artist match"
        results.append(row_to_candidate(r, tracks, confidence, reason))
    return results
