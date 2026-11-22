#!/usr/bin/env bash
# Deploy thủ công lên staging tại nhà (chạy bằng Git Bash trên Windows). Cần file .env, xem docs/staging.md.
set -euo pipefail
cd "$(dirname "$0")"

git pull --ff-only
docker compose -f compose.prod.yml up -d --build
docker compose -f compose.prod.yml ps
