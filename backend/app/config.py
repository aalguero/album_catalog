from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    CATALOG_DB_URL: str = "postgresql://andre@/vinyl_catalog?host=/var/run/postgresql"
    MUSICBRAINZ_DB_URL: str = "postgresql://andre@/musicbrainz?host=/var/run/postgresql"
    DISCOGS_TOKEN: Optional[str] = None
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
