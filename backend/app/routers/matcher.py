from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List, Optional
import asyncpg

from app.db import get_musicbrainz_conn, get_catalog_conn
from app.models.matcher import ScanRequest, ScanResponse, MatchCandidate, DiscogsPressingOption
from app.services.musicbrainz_local import (
    search_by_barcode,
    search_by_catalog_number,
    search_by_artist_title
)
from app.services.musicbrainz_remote import search_remote_musicbrainz
from app.services.discogs_client import fetch_discogs_pressings, search_discogs_direct
from app.services.ranker import extract_potential_catno, rank_and_select_top_candidates
from app.services.catalog_checker import find_existing_albums

router = APIRouter(prefix="/api/match", tags=["Matcher"])

@router.post("/scan", response_model=ScanResponse)
async def scan_metadata(
    req: ScanRequest,
    conn: asyncpg.Connection = Depends(get_musicbrainz_conn),
    catalog_conn: asyncpg.Connection = Depends(get_catalog_conn)
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
            else:
                # Fallback to direct Discogs search
                candidates = await search_discogs_direct(barcode=req.barcode)
                if candidates:
                    source_used = "discogs"

    # 2. Catalog Number and/or Artist/Title and/or OCR Text
    else:
        # Check if catalog number provided
        if req.catalog_number:
            query_type = "catalog_number"
            candidates = await search_by_catalog_number(conn, req.catalog_number)
            if candidates:
                source_used = "local_musicbrainz"
            else:
                candidates = await search_remote_musicbrainz(
                    catalog_number=req.catalog_number,
                    artist=req.artist,
                    title=req.title
                )
                if candidates:
                    source_used = "remote_musicbrainz"
                else:
                    candidates = await search_discogs_direct(
                        catno=req.catalog_number,
                        artist=req.artist,
                        title=req.title
                    )
                    if candidates:
                        source_used = "discogs"

        # Fallback to Artist and/or Title if catalog number produced no results or was absent
        if not candidates and (req.artist or req.title):
            query_type = "artist_title"
            if req.artist and req.title:
                candidates = await search_by_artist_title(conn, req.artist, req.title)
                if candidates:
                    source_used = "local_musicbrainz"

            if not candidates:
                candidates = await search_remote_musicbrainz(artist=req.artist, title=req.title)
                if candidates:
                    source_used = "remote_musicbrainz"
                else:
                    candidates = await search_discogs_direct(
                        artist=req.artist,
                        title=req.title,
                        query=f"{req.artist or ''} {req.title or ''}".strip() or None
                    )
                    if candidates:
                        source_used = "discogs"

        # Fallback to Center label / text OCR if still no candidates
        if not candidates and req.spine_text:
            query_type = "label_text"
            extracted_catno = extract_potential_catno(req.spine_text)
            if extracted_catno:
                candidates = await search_by_catalog_number(conn, extracted_catno)
                if candidates:
                    source_used = "local_musicbrainz"
            
            if not candidates:
                candidates = await search_remote_musicbrainz(
                    catalog_number=extracted_catno,
                    title=req.spine_text
                )
                if candidates:
                    source_used = "remote_musicbrainz"
                else:
                    candidates = await search_discogs_direct(query=req.spine_text)
                    if candidates:
                        source_used = "discogs"

    top3 = rank_and_select_top_candidates(
        candidates,
        query_barcode=req.barcode,
        query_catno=req.catalog_number or (extract_potential_catno(req.spine_text) if req.spine_text else None),
        query_text=req.spine_text or (f"{req.artist or ''} {req.title or ''}".strip()),
        limit=3
    )

    # Check collection for duplicates
    for cand in top3:
        existing_rows, _ = await find_existing_albums(
            conn=catalog_conn,
            barcode=cand.barcode,
            catalog_number=cand.catalog_number,
            artist=cand.artist,
            title=cand.title,
            discogs_release_id=cand.discogs_release_id,
            musicbrainz_release_id=cand.musicbrainz_release_id
        )
        if existing_rows:
            cand.in_collection = True
            first_copy = existing_rows[0]
            cond = first_copy.get("media_condition") or "VG+"
            shelf = first_copy.get("shelf_location")
            price = first_copy.get("purchase_price")
            details = [f"Cond: {cond}"]
            if shelf:
                details.append(f"Shelf: {shelf}")
            if price:
                details.append(f"${price:.2f}")
            cand.existing_copy_info = " • ".join(details)

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
