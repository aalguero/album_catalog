from pydantic import BaseModel, Field
from typing import Optional, List

class TrackItem(BaseModel):
    position: Optional[str] = None
    title: str
    duration: Optional[str] = None
    is_favorite: bool = False

class ScanRequest(BaseModel):
    barcode: Optional[str] = None
    spine_text: Optional[str] = None
    catalog_number: Optional[str] = None
    artist: Optional[str] = None
    title: Optional[str] = None

class MatchCandidate(BaseModel):
    title: str
    artist: str
    release_year: Optional[int] = None
    label: Optional[str] = None
    catalog_number: Optional[str] = None
    barcode: Optional[str] = None
    format: Optional[str] = "Vinyl, LP"
    country: Optional[str] = None
    musicbrainz_release_id: Optional[str] = None
    discogs_release_id: Optional[int] = None
    discogs_master_id: Optional[int] = None
    cover_image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    tracks: List[TrackItem] = Field(default_factory=list)
    source: str = "local_musicbrainz"  # local_musicbrainz, remote_musicbrainz, discogs
    confidence_score: int = 100  # 0 to 100
    match_reason: str = "Exact barcode match"

class ScanResponse(BaseModel):
    query_type: str
    source_used: str
    candidates: List[MatchCandidate] = Field(default_factory=list)

class DiscogsPressingOption(BaseModel):
    discogs_id: int
    title: str
    artist: str
    year: Optional[int] = None
    country: Optional[str] = None
    format_description: Optional[str] = None
    label: Optional[str] = None
    catalog_number: Optional[str] = None
    cover_image: Optional[str] = None
    notes: Optional[str] = None
