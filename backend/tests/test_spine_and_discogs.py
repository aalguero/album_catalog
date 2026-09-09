import asyncio
import httpx
from app.main import app
from app.db import init_pools, close_pools

async def run_tests():
    await init_pools()
    try:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            # 1. Test spine text OCR match
            spine_ocr_text = "IGORRR SPIRITUALITY AND DISTORTION 3984-15690-1 STEREO"
            print(f"[1] Testing Spine OCR Text: '{spine_ocr_text}'")
            resp = await client.post("/api/match/scan", json={"spine_text": spine_ocr_text})
            print(f"    Status: {resp.status_code}")
            data = resp.json()
            print(f"    Source Used: {data['source_used']}")
            for c in data['candidates']:
                print(f"    Candidate: {c['artist']} - {c['title']} | Score: {c['confidence_score']}% | Reason: {c['match_reason']}")
            assert len(data['candidates']) > 0

            # 2. Test Discogs pressing lookup
            print("\n[2] Testing Discogs Pressing Lookup:")
            resp = await client.get("/api/match/discogs-pressings", params={
                "artist": "Igorrr",
                "title": "Spirituality and Distortion"
            })
            print(f"    Status: {resp.status_code}")
            pressings = resp.json()
            print(f"    Received {len(pressings)} Discogs vinyl pressing options.")
            for p in pressings[:3]:
                print(f"      - ID {p['discogs_id']}: {p['year']} | {p['country']} | {p['format_description']} | {p['label']} ({p['catalog_number']})")

        print("\n=== Spine & Discogs Tests Completed! ===")
    finally:
        await close_pools()

if __name__ == "__main__":
    asyncio.run(run_tests())
