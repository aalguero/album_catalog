import re
from typing import List, Optional
from rapidfuzz import fuzz
from app.models.matcher import MatchCandidate

# Common catalog number patterns on vinyl spines:
# 1. Hyphenated / slashed codes (e.g. 3984-15690-1, SMAS-2653, K-50062)
# 2. Letter prefix + number (e.g. PC 34474, BST 81577, CAS 1020, ST-11234)
# 3. Label codes / numeric blocks
CATNO_PATTERN = re.compile(
    r'\b([A-Z0-9]{2,8}[-\s/][A-Z0-9]{1,8}(?:[-\s/][A-Z0-9]{1,4})?|[A-Z]{1,5}[-\s]?[0-9]{3,7})\b',
    re.IGNORECASE
)

# Words frequently on vinyl spines to ignore as catalog numbers
STOPWORDS = {"STEREO", "MONO", "RECORD", "RECORDS", "MUSIC", "SOUND", "SERIES", "ALBUM", "VOL", "VOLUME"}

def extract_potential_catno(spine_text: str) -> Optional[str]:
    matches = CATNO_PATTERN.findall(spine_text)
    for m in matches:
        cleaned = m.strip().upper()
        if cleaned not in STOPWORDS and len(cleaned) >= 4:
            return m.strip()
    return None

def rank_and_select_top_candidates(
    candidates: List[MatchCandidate],
    query_barcode: Optional[str] = None,
    query_catno: Optional[str] = None,
    query_text: Optional[str] = None,
    limit: int = 3
) -> List[MatchCandidate]:
    if not candidates:
        return []

    scored_candidates = []
    seen_keys = set()

    for cand in candidates:
        # Deduplication key
        key = (cand.artist.lower().strip(), cand.title.lower().strip(), cand.release_year, cand.catalog_number)
        if key in seen_keys:
            continue
        seen_keys.add(key)

        score = cand.confidence_score

        # Boost if format is explicitly Vinyl
        if cand.format and "vinyl" in cand.format.lower():
            score = min(100, score + 5)

        # If query_catno matched closely
        if query_catno and cand.catalog_number:
            sim = fuzz.ratio(query_catno.upper().replace(" ", ""), cand.catalog_number.upper().replace(" ", ""))
            if sim > 90:
                score = max(score, 90)

        # If text search matched artist/title
        if query_text:
            text_target = f"{cand.artist} {cand.title}"
            sim = fuzz.token_set_ratio(query_text, text_target)
            if sim > 85:
                score = max(score, int(sim * 0.95))

        cand.confidence_score = min(100, max(1, score))
        scored_candidates.append(cand)

    # Sort descending by confidence score, then by has_artwork
    scored_candidates.sort(
        key=lambda c: (c.confidence_score, 1 if c.cover_image_url else 0),
        reverse=True
    )

    return scored_candidates[:limit]
