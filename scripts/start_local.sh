#!/usr/bin/env bash
# ==============================================================================
# All-In-One Local End-To-End Launcher
# Starts: PostgreSQL check -> Spring Boot (8080) -> Streamlit UI (8501)
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=================================================================="
echo "Starting Restaurant Recommendation Engine (Local End-to-End)"
echo "=================================================================="

# 1. Check PostgreSQL on port 5432
echo -n "Checking PostgreSQL on port 5432... "
if nc -z localhost 5432 2>/dev/null || lsof -i :5432 >/dev/null 2>&1; then
    echo "[OK] Online"
else
    echo "[FAIL] Offline!"
    echo "Please start PostgreSQL before proceeding."
    echo "  - Via Docker: docker compose up -d db"
    echo "  - Via Homebrew: brew services start postgresql@16"
    exit 1
fi

# 2. Check or Start Spring Boot on port 8080
if lsof -i :8080 >/dev/null 2>&1; then
    echo "[INFO] Spring Boot backend already running on port 8080"
else
    echo "[INFO] Launching Spring Boot backend on port 8080..."
    nohup mvn spring-boot:run > "${ROOT_DIR}/target/spring-boot.log" 2>&1 &
    BACKEND_PID=$!
    echo "Backend PID: $BACKEND_PID (Logs: target/spring-boot.log)"

    echo -n "Waiting for Spring Boot readiness..."
    for i in {1..30}; do
        if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
            echo " Ready!"
            break
        fi
        echo -n "."
        sleep 2
    done
fi

# 3. Check or Start Streamlit on port 8501
if lsof -i :8501 >/dev/null 2>&1; then
    echo "[INFO] Streamlit UI already running on port 8501"
else
    echo "[INFO] Launching Streamlit UI on port 8501..."
    nohup streamlit run demo/app.py --server.port 8501 --server.headless true > "${ROOT_DIR}/target/streamlit.log" 2>&1 &
    STREAMLIT_PID=$!
    echo "Streamlit PID: $STREAMLIT_PID (Logs: target/streamlit.log)"

    echo -n "Waiting for Streamlit..."
    for i in {1..15}; do
        if curl -s -I http://localhost:8501 | grep -q "200 OK"; then
            echo " Ready!"
            break
        fi
        echo -n "."
        sleep 1
    done
fi

echo ""
echo "=================================================================="
echo "SYSTEM ONLINE AND READY"
echo "=================================================================="
echo "  • Streamlit Web UI:   http://localhost:8501"
echo "  • Swagger UI:         http://localhost:8080/swagger-ui/index.html"
echo "  • Actuator Health:    http://localhost:8080/actuator/health"
echo "=================================================================="

# Try to automatically open the browser on macOS
if command -v open >/dev/null 2>&1; then
    open "http://localhost:8501"
fi
