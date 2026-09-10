from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime, date
from app.models.matcher import TrackItem

class AlbumCreate(BaseModel):
    title: str
    artist: str
    release_year: Optional[int] = None
    label: Optional[str] = None
    catalog_number: Optional[str] = None
    barcode: Optional[str] = None
    format: Optional[str] = "Vinyl, LP"
    country: Optional[str] = None
    
    pressing_status: str = "unspecific"  # specific or unspecific
    metadata_source: str = "local_musicbrainz"  # local_musicbrainz, remote_musicbrainz, discogs, manual
    
    musicbrainz_release_id: Optional[str] = None
    discogs_release_id: Optional[int] = None
    discogs_master_id: Optional[int] = None
    cover_image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    
    media_condition: Optional[str] = None  # M, NM, VG+, VG, G+, G, F, P
    sleeve_condition: Optional[str] = None
    purchase_price: Optional[float] = None
    purchase_date: Optional[date] = None
    purchase_store: Optional[str] = None
    shelf_location: Optional[str] = None
    notes: Optional[str] = None
    favorite_tracks: List[str] = Field(default_factory=list)
    tags: List[str] = Field(default_factory=list)
    tracks: List[TrackItem] = Field(default_factory=list)

class AlbumUpdate(BaseModel):
    title: Optional[str] = None
    artist: Optional[str] = None
    release_year: Optional[int] = None
    label: Optional[str] = None
    catalog_number: Optional[str] = None
    barcode: Optional[str] = None
    format: Optional[str] = None
    country: Optional[str] = None
    pressing_status: Optional[str] = None
    metadata_source: Optional[str] = None
    musicbrainz_release_id: Optional[str] = None
    discogs_release_id: Optional[int] = None
    discogs_master_id: Optional[int] = None
    cover_image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    media_condition: Optional[str] = None
    sleeve_condition: Optional[str] = None
    purchase_price: Optional[float] = None
    purchase_date: Optional[date] = None
    purchase_store: Optional[str] = None
    shelf_location: Optional[str] = None
    notes: Optional[str] = None
    favorite_tracks: Optional[List[str]] = None
    tags: Optional[List[str]] = None

class AlbumResponse(AlbumCreate):
    id: int
    created_at: datetime
    updated_at: datetime
