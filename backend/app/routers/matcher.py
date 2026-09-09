from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List, Optional
import asyncpg

from app.db import get_musicbrainz_conn
from app.models.matcher import ScanRequest, ScanResponse, MatchCandidate, DiscogsPressingOption
from app.services.musicbrainz_local import (
    search_by_barcode,
    search_by_catalog_number,
    search_by_artist_title
)
from app.services.musicbrainz_remote import search_remote_musicbrainz
from app.services.discogs_client import fetch_discogs_pressings
from app.services.ranker import extract_potential_catno, rank_and_select_top_candidates

router = APIRouter(prefix="/api/match", tags=["Matcher"])

@router.post("/scan", response_model=ScanResponse)
async def scan_metadata(
    req: ScanRequest,
    conn: asyncpg.Connection = Depends(get_musicbrainz_conn)
):
    candidates: List[MatchCandidate] = []
    source_used = "none"
    query_type = "search"

    # 1. Barcode scanning (highest priority & instant)
    if req.barcode:
        query_type = "barcode"
        candidates = await search_by_barcode(conn, req.barcode)
        if candidates:
            source_used = "local_musicbrainz"
        else:
            # Fallback to remote MusicBrainz
            candidates = await search_remote_musicbrainz(barcode=req.barcode)
            if candidates:
                source_used = "remote_musicbrainz"

    # 2. Catalog Number
    elif req.catalog_number:
        query_type = "catalog_number"
        candidates = await search_by_catalog_number(conn, req.catalog_number)
        if candidates:
            source_used = "local_musicbrainz"
        else:
            candidates = await search_remote_musicbrainz(catalog_number=req.catalog_number)
            if candidates:
                source_used = "remote_musicbrainz"

    # 3. Spine text OCR
    elif req.spine_text:
        query_type = "spine_text"
        extracted_catno = extract_potential_catno(req.spine_text)
        if extracted_catno:
            candidates = await search_by_catalog_number(conn, extracted_catno)
            if candidates:
                source_used = "local_musicbrainz"
        
        # If no catno match or extra text, try remote or token search
        if not candidates:
            candidates = await search_remote_musicbrainz(
                catalog_number=extracted_catno,
                title=req.spine_text
            )
            if candidates:
                source_used = "remote_musicbrainz"

    # 4. Explicit Artist + Title
    elif req.artist and req.title:
        query_type = "artist_title"
        candidates = await search_by_artist_title(conn, req.artist, req.title)
        if candidates:
            source_used = "local_musicbrainz"
        else:
            candidates = await search_remote_musicbrainz(artist=req.artist, title=req.title)
            if candidates:
                source_used = "remote_musicbrainz"

    top3 = rank_and_select_top_candidates(
        candidates,
        query_barcode=req.barcode,
        query_catno=req.catalog_number or (extract_potential_catno(req.spine_text) if req.spine_text else None),
        query_text=req.spine_text or (f"{req.artist or ''} {req.title or ''}".strip()),
        limit=3
    )

    return ScanResponse(
        query_type=query_type,
        source_used=source_used,
        candidates=top3
    )

@router.get("/discogs-pressings", response_model=List[DiscogsPressingOption])
async def get_discogs_pressings(
    artist: str = Query(..., description="Artist name from preliminary match"),
    title: str = Query(..., description="Album title from preliminary match"),
    catalog_number: Optional[str] = Query(None, description="Catalog number if available"),
    barcode: Optional[str] = Query(None, description="Barcode if available")
):
    pressings = await fetch_discogs_pressings(
        artist=artist,
        title=title,
        catalog_number=catalog_number,
        barcode=barcode
    )
    return pressings
