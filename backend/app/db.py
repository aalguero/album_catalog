import asyncpg
from typing import Optional
from app.config import settings

catalog_pool: Optional[asyncpg.Pool] = None
musicbrainz_pool: Optional[asyncpg.Pool] = None

async def init_pools():
    global catalog_pool, musicbrainz_pool
    if catalog_pool is None:
        catalog_pool = await asyncpg.create_pool(
            dsn=settings.CATALOG_DB_URL,
            min_size=2,
            max_size=10
        )
    if musicbrainz_pool is None:
        musicbrainz_pool = await asyncpg.create_pool(
            dsn=settings.MUSICBRAINZ_DB_URL,
            min_size=2,
            max_size=10
        )

async def close_pools():
    global catalog_pool, musicbrainz_pool
    if catalog_pool:
        await catalog_pool.close()
        catalog_pool = None
    if musicbrainz_pool:
        await musicbrainz_pool.close()
        musicbrainz_pool = None

async def get_catalog_conn():
    assert catalog_pool is not None, "Catalog DB pool not initialized"
    async with catalog_pool.acquire() as connection:
        yield connection

async def get_musicbrainz_conn():
    assert musicbrainz_pool is not None, "MusicBrainz DB pool not initialized"
    async with musicbrainz_pool.acquire() as connection:
        yield connection
