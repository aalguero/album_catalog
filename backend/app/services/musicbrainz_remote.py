import httpx
from typing import List, Optional
from app.models.matcher import MatchCandidate, TrackItem

MB_BASE_URL = "https://musicbrainz.org/ws/2"
HEADERS = {
    "User-Agent": "VinylAlbumCatalogApp/1.0 ( andre-vinyl-catalog )",
    "Accept": "application/json"
}

async def search_remote_musicbrainz(
    barcode: Optional[str] = None,
    catalog_number: Optional[str] = None,
    artist: Optional[str] = None,
    title: Optional[str] = None
) -> List[MatchCandidate]:
    query_parts = []
    if barcode:
        query_parts.append(f'barcode:"{barcode.strip()}"')
    if catalog_number:
        query_parts.append(f'catno:"{catalog_number.strip()}"')
    if artist:
        query_parts.append(f'artist:"{artist.strip()}"')
    if title:
        query_parts.append(f'release:"{title.strip()}"')
    
    if not query_parts:
        return []
    
    query = " AND ".join(query_parts)
    url = f"{MB_BASE_URL}/release"
    params = {
        "query": query,
        "fmt": "json",
        "limit": 3
    }
    
    try:
        async with httpx.AsyncClient(headers=HEADERS, timeout=8.0) as client:
            resp = await client.get(url, params=params)
            if resp.status_code != 200:
                return []
            data = resp.json()
            releases = data.get("releases", [])
            
            candidates = []
            for r in releases:
                gid = r.get("id")
                artist_credit = r.get("artist-credit", [])
                artist_name = artist_credit[0].get("name", "Unknown Artist") if artist_credit else "Unknown Artist"
                date_str = r.get("date", "")
                year = int(date_str[:4]) if date_str and date_str[:4].isdigit() else None
                
                # Format & label
                media = r.get("media", [])
                format_name = media[0].get("format", "Vinyl") if media else "Vinyl"
                label_info = r.get("label-info", [])
                label_name = label_info[0].get("label", {}).get("name") if label_info else None
                cat_no = label_info[0].get("catalog-number") if label_info else None
                
                tracks = []
                if media and "tracks" in media[0]:
                    for t in media[0]["tracks"]:
                        tracks.append(TrackItem(
                            position=t.get("number"),
                            title=t.get("title", ""),
                            duration=None
                        ))
                
                cover_url = f"https://coverartarchive.org/release/{gid}/front-500" if gid else None
                thumb_url = f"https://coverartarchive.org/release/{gid}/front-250" if gid else None
                
                candidates.append(MatchCandidate(
                    title=r.get("title", ""),
                    artist=artist_name,
                    release_year=year,
                    label=label_name,
                    catalog_number=cat_no or catalog_number,
                    barcode=r.get("barcode") or barcode,
                    format=format_name,
                    country=r.get("country"),
                    musicbrainz_release_id=gid,
                    cover_image_url=cover_url,
                    thumbnail_url=thumb_url,
                    tracks=tracks,
                    source="remote_musicbrainz",
                    confidence_score=75,
                    match_reason="Remote MusicBrainz fallback match"
                ))
            return candidates
    except Exception:
        return []
