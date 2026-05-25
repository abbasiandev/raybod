#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../backend"

export TESTING=1
export SKIP_INIT_DB=1

python -m pytest tests/ \
  -v \
  --tb=short \
  --ignore=tests/test_auth.py \
  --ignore=tests/test_billing.py \
  --ignore=tests/test_dashboard.py \
  "$@"
