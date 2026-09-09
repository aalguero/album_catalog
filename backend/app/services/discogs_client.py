import httpx
from typing import List, Optional
from app.config import settings
from app.models.matcher import DiscogsPressingOption

DISCOGS_BASE_URL = "https://api.discogs.com"

async def fetch_discogs_pressings(
    artist: str,
    title: str,
    catalog_number: Optional[str] = None,
    barcode: Optional[str] = None
) -> List[DiscogsPressingOption]:
    headers = {
        "User-Agent": "VinylAlbumCatalog/1.0 +https://github.com/aalguero/album_catalog"
    }
    if settings.DISCOGS_TOKEN:
        headers["Authorization"] = f"Discogs token={settings.DISCOGS_TOKEN}"
        
    params = {
        "type": "release",
        "format": "Vinyl",
        "artist": artist.strip(),
        "release_title": title.strip(),
        "per_page": 10
    }
    if barcode:
        params["barcode"] = barcode.strip()
    elif catalog_number:
        params["catno"] = catalog_number.strip()

    try:
        async with httpx.AsyncClient(headers=headers, timeout=10.0) as client:
            resp = await client.get(f"{DISCOGS_BASE_URL}/database/search", params=params)
            if resp.status_code != 200:
                return []
            data = resp.json()
            results = data.get("results", [])
            
            pressings = []
            for item in results:
                # Format description e.g. "Vinyl, LP, Album, Stereo"
                formats = item.get("format", [])
                format_desc = ", ".join(formats) if isinstance(formats, list) else str(formats)
                
                # Label and Cat No
                label_list = item.get("label", [])
                label_name = label_list[0] if label_list else None
                cat_no = item.get("catno")
                
                year_val = None
                if item.get("year"):
                    try:
                        year_val = int(item.get("year"))
                    except ValueError:
                        pass
                
                pressings.append(DiscogsPressingOption(
                    discogs_id=item.get("id"),
                    title=item.get("title", title),
                    artist=artist,
                    year=year_val,
                    country=item.get("country"),
                    format_description=format_desc,
                    label=label_name,
                    catalog_number=cat_no or catalog_number,
                    cover_image=item.get("cover_image") or item.get("thumb"),
                    notes=None
                ))
            return pressings
    except Exception:
        return []
