#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "=== Starting Vinyl Catalog FastAPI Backend on 0.0.0.0:8000 ==="
export PYTHONPATH=backend
exec backend/.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
