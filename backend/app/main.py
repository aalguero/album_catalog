from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.db import init_pools, close_pools
from app.routers import matcher, catalog

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Initialize asyncpg connection pools
    await init_pools()
    yield
    # Shutdown: Close pools cleanly
    await close_pools()

app = FastAPI(
    title="Vinyl Record Catalog API",
    description="High-speed vinyl record matching and collection management API",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(matcher.router)
app.include_router(catalog.router)

@app.get("/")
async def root():
    return {
        "status": "online",
        "service": "Vinyl Record Catalog Backend",
        "version": "1.0.0"
    }

@app.get("/health")
@app.get("/api/health")
async def health():
    return {"status": "ok"}
