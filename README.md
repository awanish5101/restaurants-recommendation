# 🍽️ AI Restaurant Recommendation Engine

[![CI](https://github.com/awanish5101/restaurants-recommendation/actions/workflows/ci.yml/badge.svg)](https://github.com/awanish5101/restaurants-recommendation/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-blue.svg)](https://github.com/pgvector/pgvector)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A production-grade, RAG-powered restaurant recommendation engine combining **PostgreSQL + pgvector** semantic retrieval, **Spring Boot 3**, **Spring AI / Google Gemini**, and an interactive **Streamlit** geospatial frontend.

Designed with enterprise resilience principles: hybrid spatial + vector filtering, structured LLM rationale generation, anti-hallucination verification, deterministic fallback degradation, query embedding caching, and automated benchmark evaluation.

---

## 🏗️ Architecture Overview

```
                      ┌─────────────────────────────────┐
                      │    Streamlit Geospatial Demo    │
                      │       (Folium Map + KPIs)       │
                      └────────────────┬────────────────┘
                                       │ HTTP / REST
                                       ▼
                      ┌─────────────────────────────────┐
                      │   Spring Boot 3 REST Service    │
                      │  (OpenAPI 3 / Actuator / Cache) │
                      └────────┬───────────────┬────────┘
                               │               │
            Hybrid Semantic &  │               │ Structured Prompts &
            Spatial Retrieval  │               │ Grounded Rationales
                               ▼               ▼
         ┌───────────────────────────┐   ┌───────────────────────────┐
         │   PostgreSQL + pgvector   │   │     Google Gemini LLM     │
         │ - HNSW Cosine Similarity  │   │  (gemini-2.5-flash /      │
         │ - Haversine Distance      │   │   Rule-Based Fallback)    │
         │ - Rating & Price Filters  │   └───────────────────────────┘
         └───────────────────────────┘
```

### Retrieval & Generation Pipeline
1. **Semantic Query Embedding**: The user's dining preferences and vibe (e.g., *"romantic Italian patio with handmade pasta"*) are vectorized via Gemini embeddings (`text-embedding-004` / `gemini-embedding-001`, 768 dimensions) with Caffeine in-memory caching.
2. **Hybrid pgvector Retrieval**: Candidate restaurants are retrieved via HNSW vector index cosine similarity combined with relational bounding predicates (haversine spatial distance, minimum rating, and price tier).
3. **Structured Justification**: Gemini generates concise, personalized dining rationales anchored strictly in candidate metadata.
4. **Anti-Hallucination & Fallback Guardrails**: Recommendations are strictly mapped against verified database candidates. If the LLM is unreachable, quota-throttled, or offline, the system seamlessly triggers deterministic, rule-based rationales without downtime or 5xx errors.

---

## ✨ Key Features

- **Hybrid Spatial + Vector Retrieval**: Intersects vector similarity with spatial constraints ($\le$ max distance in km) and relational filters in a single query path.
- **Resilient Fallback Mode**: Works 100% offline out-of-the-box using deterministic rule-based rationales when no Gemini API key is supplied.
- **Query Embedding Caching**: Caffeine in-memory cache eliminates redundant API roundtrips for repeated searches.
- **Interactive Geospatial UI**: Streamlit web application featuring Folium interactive maps, search radius visualizer, ranked restaurant cards, KPI metrics, and diner visit/rating logs.
- **Automated RAG Evaluation Suite**: End-to-end benchmark script (`scripts/eval_rag.py`) measuring distance adherence, rating constraints, rationale groundedness, and latency percentiles.
- **Production Observability & Docs**: OpenAPI 3 / Swagger UI at `/swagger-ui/index.html` and health probes via Spring Boot Actuator at `/actuator/health`.
- **Docker & CI Ready**: Full `docker-compose.yml` setup and GitHub Actions CI workflow with a live pgvector service container.

---

## 📊 RAG Benchmark & Evaluation Results

Evaluated across diverse query personas using `scripts/eval_rag.py`:

| Quality Metric | Benchmark Target | Achieved | Status |
| :--- | :---: | :---: | :---: |
| **API Success Rate** | 100% | **100.0%** | ✅ PASS |
| **Distance Constraint Adherence** | 100% | **100.0%** | ✅ PASS |
| **Rating Quality Adherence** | 100% | **100.0%** | ✅ PASS |
| **Rationale Groundedness / Factuality** | > 95% | **100.0%** | ✅ PASS |
| **P50 Query Latency** | < 1500 ms | **16.3 ms** | ✅ PASS |
| **P90 Query Latency** | < 3000 ms | **37.3 ms** | ✅ PASS |

*Detailed benchmark metrics and sample query breakdowns are available in [docs/rag_eval_report.md](docs/rag_eval_report.md).*

---

## 🚀 Quickstart

### Prerequisites
- Java 21+ & Maven 3.9+ (or Docker & Docker Compose)
- PostgreSQL 16 with the `pgvector` extension enabled
- Python 3.10+ (for the Streamlit demo and eval suite)

### 1. Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/awanish5101/restaurants-recommendation.git
cd restaurants-recommendation/restaurantsystem

# (Optional) Provide Gemini API key in .env or shell
export GEMINI_API_KEY="your-gemini-api-key"

# Launch Postgres+pgvector and Spring Boot service
docker compose up --build
```

The service will start at `http://localhost:8080`.

### 2. Run Locally with Maven

```bash
# Ensure PostgreSQL is running on port 5432
mvn clean package -DskipTests
mvn spring-boot:run
```

The database schema is automatically migrated using Flyway, and the sample catalog of 1,500 restaurants is seeded on first boot.

### 3. Launch the Streamlit Interactive UI

```bash
# In a separate terminal
./scripts/run_demo.sh
# Or manually:
pip install -r demo/requirements.txt
streamlit run demo/app.py
```

Open your browser at `http://localhost:8501`.

---

## 🔌 API Reference

### 1. Recommend Restaurants
`POST /api/restaurants/recommend?latitude={lat}&longitude={lon}`

**Request Body:**
```json
{
  "preferredCuisine": "authentic Japanese sushi and ramen",
  "maxDistanceInKm": 10.0,
  "prioritizeRating": true,
  "preferredPriceRange": 2,
  "minimumRating": 4
}
```

**Response (HTTP 200):**
```json
[
  {
    "id": 175163,
    "restaurantName": "Yama Ramen and Sushi",
    "justification": "Yama Ramen and Sushi serves authentic Sushi & Japanese dishes, rated 4.6/5, located 0.4 km away.",
    "distance": 0.42,
    "overallRating": 4.6,
    "priceRange": 2,
    "cuisines": ["Sushi", "Japanese"],
    "latitude": 40.7589,
    "longitude": -73.9841
  }
]
```

### 2. Log Diner Visit
`POST /history/visit?userId={userId}&restaurantId={restaurantId}`

**Response:**
```json
{
  "status": "ok",
  "message": "Visit logged."
}
```

### 3. Log Diner Rating
`POST /history/rate?userId={userId}&restaurantId={restaurantId}&rating=4.5`

**Response:**
```json
{
  "status": "ok",
  "message": "Rating logged."
}
```

### 4. Interactive Documentation
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 🧪 Testing & Evaluation

### Run Test Suite
```bash
mvn clean verify
```
Runs unit tests, Mockito resilience tests, and full slice integration tests with embedded Flyway migrations.

### Run RAG Evaluation Benchmark
```bash
python3 scripts/eval_rag.py --url http://localhost:8080 --output docs/rag_eval_report.md
```

---

## ⚙️ Configuration Reference

| Environment Variable | Description | Default |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://localhost:5432/restaurantdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `restaurant` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `restaurant` |
| `GEMINI_API_KEY` | Google Gemini API Key | *(empty - enables rule-based fallback)* |
| `APP_INGESTION_ENABLED` | Ingest bundled sample data on startup | `true` |
| `APP_EMBEDDING_ENABLED` | Run background embedding backfill | `true` |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
