from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List, Optional
import asyncpg
import json
import uuid

from app.db import get_catalog_conn
from app.models.catalog import AlbumCreate, AlbumUpdate, AlbumResponse, DuplicateCheckRequest, DuplicateCheckResponse
from app.models.matcher import TrackItem
from app.services.catalog_checker import find_existing_albums

router = APIRouter(prefix="/api/catalog", tags=["Catalog"])

@router.post("/albums", response_model=AlbumResponse)
async def add_album(album: AlbumCreate, conn: asyncpg.Connection = Depends(get_catalog_conn)):
    query = """
    INSERT INTO albums (
        title, artist, release_year, label, catalog_number, barcode, format, country,
        pressing_status, metadata_source, musicbrainz_release_id, discogs_release_id,
        discogs_master_id, cover_image_url, thumbnail_url,
        media_condition, sleeve_condition, purchase_price, purchase_date, purchase_store,
        shelf_location, notes, favorite_tracks, tags
    ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8,
        $9, $10, $11, $12,
        $13, $14, $15,
        $16, $17, $18, $19, $20,
        $21, $22, $23, $24
    ) RETURNING id, created_at, updated_at;
    """
    mb_uuid = uuid.UUID(album.musicbrainz_release_id) if album.musicbrainz_release_id else None

    row = await conn.fetchrow(
        query,
        album.title, album.artist, album.release_year, album.label, album.catalog_number, album.barcode,
        album.format, album.country, album.pressing_status, album.metadata_source,
        mb_uuid, album.discogs_release_id, album.discogs_master_id,
        album.cover_image_url, album.thumbnail_url,
        album.media_condition, album.sleeve_condition, album.purchase_price, album.purchase_date, album.purchase_store,
        album.shelf_location, album.notes, album.favorite_tracks, album.tags
    )
    album_id = row["id"]

    # Insert tracks if provided
    if album.tracks:
        track_query = """
        INSERT INTO tracks (album_id, position, title, duration, is_favorite)
        VALUES ($1, $2, $3, $4, $5);
        """
        for t in album.tracks:
            await conn.execute(track_query, album_id, t.position, t.title, t.duration, t.is_favorite)

    return AlbumResponse(
        id=album_id,
        created_at=row["created_at"],
        updated_at=row["updated_at"],
        **album.model_dump()
    )

@router.get("/albums", response_model=List[AlbumResponse])
async def list_albums(
    q: Optional[str] = Query(None, description="Search artist or title"),
    pressing_status: Optional[str] = Query(None, description="Filter by 'specific' or 'unspecific'"),
    shelf_location: Optional[str] = Query(None, description="Filter by shelf location"),
    limit: int = Query(50, le=100),
    offset: int = Query(0),
    conn: asyncpg.Connection = Depends(get_catalog_conn)
):
    clauses = []
    args = []
    
    if q:
        args.append(f"%{q.strip()}%")
        clauses.append(f"(title ILIKE ${len(args)} OR artist ILIKE ${len(args)})")
    if pressing_status:
        args.append(pressing_status)
        clauses.append(f"pressing_status = ${len(args)}")
    if shelf_location:
        args.append(shelf_location)
        clauses.append(f"shelf_location = ${len(args)}")

    where_str = f"WHERE {' AND '.join(clauses)}" if clauses else ""
    
    args.append(limit)
    limit_pos = len(args)
    args.append(offset)
    offset_pos = len(args)

    query = f"""
    SELECT 
        id, title, artist, release_year, label, catalog_number, barcode, format, country,
        pressing_status, metadata_source, musicbrainz_release_id, discogs_release_id,
        discogs_master_id, cover_image_url, thumbnail_url,
        media_condition, sleeve_condition, purchase_price, purchase_date, purchase_store,
        shelf_location, notes, favorite_tracks, tags, created_at, updated_at
    FROM albums
    {where_str}
    ORDER BY id DESC
    LIMIT ${limit_pos} OFFSET ${offset_pos};
    """
    rows = await conn.fetch(query, *args)
    
    results = []
    for r in rows:
        album_dict = dict(r)
        if album_dict.get("musicbrainz_release_id"):
            album_dict["musicbrainz_release_id"] = str(album_dict["musicbrainz_release_id"])
        
        # Fetch tracks
        track_rows = await conn.fetch(
            "SELECT position, title, duration, is_favorite FROM tracks WHERE album_id = $1 ORDER BY id;",
            r["id"]
        )
        album_dict["tracks"] = [TrackItem(**dict(tr)) for tr in track_rows]
        results.append(AlbumResponse(**album_dict))
    
    return results

@router.get("/albums/{album_id}", response_model=AlbumResponse)
async def get_album(album_id: int, conn: asyncpg.Connection = Depends(get_catalog_conn)):
    row = await conn.fetchrow("SELECT * FROM albums WHERE id = $1;", album_id)
    if not row:
        raise HTTPException(status_code=404, detail="Album not found")
    
    album_dict = dict(row)
    if album_dict.get("musicbrainz_release_id"):
        album_dict["musicbrainz_release_id"] = str(album_dict["musicbrainz_release_id"])
        
    track_rows = await conn.fetch(
        "SELECT position, title, duration, is_favorite FROM tracks WHERE album_id = $1 ORDER BY id;",
        album_id
    )
    album_dict["tracks"] = [TrackItem(**dict(tr)) for tr in track_rows]
    return AlbumResponse(**album_dict)

@router.put("/albums/{album_id}", response_model=AlbumResponse)
@router.patch("/albums/{album_id}", response_model=AlbumResponse)
async def update_album(
    album_id: int,
    album_update: AlbumUpdate,
    conn: asyncpg.Connection = Depends(get_catalog_conn)
):
    existing = await conn.fetchrow("SELECT * FROM albums WHERE id = $1;", album_id)
    if not existing:
        raise HTTPException(status_code=404, detail="Album not found")

    update_data = album_update.model_dump(exclude_unset=True)
    tracks_to_update = update_data.pop("tracks", None)
    fields = []
    args = []
    
    for field_name, value in update_data.items():
        if field_name == "musicbrainz_release_id" and value:
            try:
                value = uuid.UUID(str(value))
            except ValueError:
                value = None
        # Don't overwrite existing non-null fields with None if doing partial update
        if value is None and existing.get(field_name) is not None:
            continue
        args.append(value)
        fields.append(f"{field_name} = ${len(args)}")
    
    if not fields:
        return await get_album(album_id, conn)

    fields.append("updated_at = CURRENT_TIMESTAMP")
    args.append(album_id)
    
    query = f"""
    UPDATE albums
    SET {', '.join(fields)}
    WHERE id = ${len(args)}
    RETURNING *;
    """
    row = await conn.fetchrow(query, *args)
    album_dict = dict(row)
    if album_dict.get("musicbrainz_release_id"):
        album_dict["musicbrainz_release_id"] = str(album_dict["musicbrainz_release_id"])
        
    # If tracks provided and album doesn't have tracks, insert them
    if tracks_to_update:
        existing_track_count = await conn.fetchval("SELECT count(*) FROM tracks WHERE album_id = $1;", album_id)
        if existing_track_count == 0:
            track_query = """
            INSERT INTO tracks (album_id, position, title, duration, is_favorite)
            VALUES ($1, $2, $3, $4, $5);
            """
            for t in tracks_to_update:
                await conn.execute(track_query, album_id, t.position, t.title, t.duration, t.is_favorite)

    track_rows = await conn.fetch(
        "SELECT position, title, duration, is_favorite FROM tracks WHERE album_id = $1 ORDER BY id;",
        album_id
    )
    album_dict["tracks"] = [TrackItem(**dict(tr)) for tr in track_rows]
    return AlbumResponse(**album_dict)

@router.delete("/albums/{album_id}")
async def delete_album(album_id: int, conn: asyncpg.Connection = Depends(get_catalog_conn)):
    result = await conn.execute("DELETE FROM albums WHERE id = $1;", album_id)
    if result == "DELETE 0":
        raise HTTPException(status_code=404, detail="Album not found")
    return {"message": "Album deleted successfully"}

@router.post("/check-duplicate", response_model=DuplicateCheckResponse)
async def check_duplicate(
    req: DuplicateCheckRequest,
    conn: asyncpg.Connection = Depends(get_catalog_conn)
):
    rows, reason = await find_existing_albums(
        conn=conn,
        barcode=req.barcode,
        catalog_number=req.catalog_number,
        artist=req.artist,
        title=req.title,
        discogs_release_id=req.discogs_release_id,
        musicbrainz_release_id=req.musicbrainz_release_id
    )

    if not rows:
        return DuplicateCheckResponse(is_duplicate=False, existing_albums=[])

    album_responses = []
    for r in rows:
        album_dict = dict(r)
        if album_dict.get("musicbrainz_release_id"):
            album_dict["musicbrainz_release_id"] = str(album_dict["musicbrainz_release_id"])
        
        track_rows = await conn.fetch(
            "SELECT position, title, duration, is_favorite FROM tracks WHERE album_id = $1 ORDER BY id;",
            r["id"]
        )
        album_dict["tracks"] = [TrackItem(**dict(tr)) for tr in track_rows]
        album_responses.append(AlbumResponse(**album_dict))

    return DuplicateCheckResponse(
        is_duplicate=True,
        match_reason=reason,
        existing_albums=album_responses
    )

