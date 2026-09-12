import asyncio
import httpx
from app.main import app
from app.db import init_pools, close_pools

async def run_tests():
    print("=== Testing FastAPI Backend & Local MusicBrainz Integration ===")
    
    # Initialize connection pools for test harness
    await init_pools()
    
    try:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            # 1. Health check
            resp = await client.get("/api/health")
            print(f"[1] Health Check: Status {resp.status_code}, Body: {resp.json()}")
            assert resp.status_code == 200

            # 2. Test Barcode Scan (Local MusicBrainz)
            test_barcode = "039841569018"  # Igorrr - Spirituality and Distortion (12" Vinyl)
            print(f"\n[2] Testing Barcode Scan for: {test_barcode}")
            scan_payload = {"barcode": test_barcode}
            resp = await client.post("/api/match/scan", json=scan_payload)
            print(f"    Scan Status: {resp.status_code}")
            data = resp.json()
            print(f"    Query Type: {data['query_type']}, Source Used: {data['source_used']}")
            print(f"    Found {len(data['candidates'])} candidates:")
            for idx, c in enumerate(data['candidates'], 1):
                print(f"      Candidate #{idx}: {c['artist']} - {c['title']} ({c['release_year']})")
                print(f"        Format: {c['format']}, Label: {c['label']}, Cat#: {c['catalog_number']}")
                print(f"        Confidence: {c['confidence_score']}%, Reason: {c['match_reason']}")
                if c['tracks']:
                    print(f"        Tracks: {len(c['tracks'])} tracks (First track: {c['tracks'][0]['position']} - {c['tracks'][0]['title']})")
            assert len(data['candidates']) > 0
            candidate = data['candidates'][0]

            # 3. Test Staging & Adding to vinyl_catalog with pressing_status='unspecific'
            print("\n[3] Testing Adding to Personal Catalog (pressing_status='unspecific'):")
            album_payload = {
                "title": candidate["title"],
                "artist": candidate["artist"],
                "release_year": candidate["release_year"],
                "label": candidate["label"],
                "catalog_number": candidate["catalog_number"],
                "barcode": candidate["barcode"],
                "format": candidate["format"],
                "country": candidate["country"],
                "pressing_status": "unspecific",
                "metadata_source": candidate["source"],
                "musicbrainz_release_id": candidate["musicbrainz_release_id"],
                "cover_image_url": candidate["cover_image_url"],
                "thumbnail_url": candidate["thumbnail_url"],
                "media_condition": "NM",
                "sleeve_condition": "VG+",
                "purchase_price": 28.50,
                "purchase_store": "Local Record Shop",
                "shelf_location": "Shelf A1",
                "notes": "First test record scanned via local MusicBrainz!",
                "favorite_tracks": ["Downgrade Desert", "Parpaing"],
                "tags": ["metal", "baroque", "electronic"],
                "tracks": candidate["tracks"]
            }
            resp = await client.post("/api/catalog/albums", json=album_payload)
            print(f"    Add Album Status: {resp.status_code}")
            saved_album = resp.json()
            print(f"    Saved Album ID: {saved_album['id']}, Pressing Status: {saved_album['pressing_status']}")
            print(f"    Saved Tracks: {len(saved_album['tracks'])}")
            assert resp.status_code == 200

            # 4. Test Listing Collection
            print("\n[4] Testing Listing Collection:")
            resp = await client.get("/api/catalog/albums")
            catalog_list = resp.json()
            print(f"    Catalog currently has {len(catalog_list)} albums:")
            for a in catalog_list[:5]:
                print(f"      - ID {a['id']}: {a['artist']} - {a['title']} [{a['pressing_status']}] (Condition: {a['media_condition']})")
            assert len(catalog_list) > 0

            # 5. Test Duplicate Detection Endpoint (POST /api/catalog/check-duplicate)
            print("\n[5] Testing Duplicate Detection Endpoint:")
            dup_resp = await client.post("/api/catalog/check-duplicate", json={"barcode": test_barcode})
            assert dup_resp.status_code == 200
            dup_data = dup_resp.json()
            print(f"    Duplicate Check (Barcode): is_duplicate={dup_data['is_duplicate']}, reason='{dup_data['match_reason']}'")
            print(f"    Found {len(dup_data['existing_albums'])} existing copies.")
            assert dup_data["is_duplicate"] is True
            assert len(dup_data["existing_albums"]) >= 1

            # Test duplicate check by Artist & Title
            dup_resp_artist = await client.post("/api/catalog/check-duplicate", json={
                "artist": "Igorrr",
                "title": "Spirituality and Distortion"
            })
            assert dup_resp_artist.status_code == 200
            dup_artist_data = dup_resp_artist.json()
            print(f"    Duplicate Check (Artist/Title): is_duplicate={dup_artist_data['is_duplicate']}, reason='{dup_artist_data['match_reason']}'")
            assert dup_artist_data["is_duplicate"] is True

            # Test duplicate check for non-existing album
            non_dup_resp = await client.post("/api/catalog/check-duplicate", json={"barcode": "999999999999"})
            assert non_dup_resp.status_code == 200
            assert non_dup_resp.json()["is_duplicate"] is False
            print("    Duplicate Check (Non-existing): correctly returned is_duplicate=False")

            # 6. Test Candidate Collection Enrichment in Scanner
            print("\n[6] Testing Candidate Duplicate Enrichment in /api/match/scan:")
            scan_recheck = await client.post("/api/match/scan", json={"barcode": test_barcode})
            assert scan_recheck.status_code == 200
            recheck_data = scan_recheck.json()
            top_candidate = recheck_data["candidates"][0]
            print(f"    Top Candidate: in_collection={top_candidate.get('in_collection')}, info='{top_candidate.get('existing_copy_info')}'")
            assert top_candidate.get("in_collection") is True
            assert top_candidate.get("existing_copy_info") is not None

        print("\n=== ALL INTEGRATION TESTS PASSED SUCCESSFULLY! ===")
    finally:
        await close_pools()

if __name__ == "__main__":
    asyncio.run(run_tests())
