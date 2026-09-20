#!/usr/bin/env bash
# ==============================================================================
# Cleanly stops local Spring Boot (8080) and Streamlit (8501) services
# ==============================================================================
set -euo pipefail

echo "Stopping local services..."

if lsof -ti :8501 >/dev/null 2>&1; then
    echo "Stopping Streamlit (port 8501)..."
    lsof -ti :8501 | xargs kill -9 2>/dev/null || true
fi

if lsof -ti :8080 >/dev/null 2>&1; then
    echo "Stopping Spring Boot (port 8080)..."
    lsof -ti :8080 | xargs kill -9 2>/dev/null || true
fi

echo "All local application services stopped."
