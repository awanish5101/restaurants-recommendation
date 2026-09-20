#!/usr/bin/env bash
# Quick launcher for Streamlit Demo UI
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "========================================================"
echo "Starting Restaurant Recommender Streamlit Demo"
echo "========================================================"

if ! command -v streamlit &> /dev/null; then
    echo "Streamlit not found in PATH. Installing requirements..."
    python3 -m pip install -r demo/requirements.txt
fi

streamlit run demo/app.py --server.port 8501 --server.headless false
