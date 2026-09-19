#!/usr/bin/env python3
"""
Sample a small, commit-safe subset of the full merchants export.

The full export (`bff_nonprod.merchants_23 (2).json`, ~105 MB, 18k+ rows) is far
too large for GitHub's 100 MiB limit and would make embedding the whole corpus
slow/expensive on a free-tier key. This script cuts a representative sample and
trims each record to the fields the recommender actually uses.

Density matters for the geo-distance filter, so we over-sample the single densest
metro (guaranteeing a query in that city returns plenty) and add a spread sample
from everywhere else.

Usage:
    python3 scripts/sample_data.py \
        --source "../bff_nonprod.merchants_23 (2).json" \
        --out src/main/resources/data/restaurants.sample.json \
        --size 1500
"""
import argparse
import json
import random
from collections import Counter
from pathlib import Path

SEED = 42  # deterministic output


def usable(r: dict) -> bool:
    geo = r.get("geoLocation") or {}
    coords = geo.get("coordinates") or []
    return (
        len(coords) == 2
        and isinstance(coords[0], (int, float))
        and isinstance(coords[1], (int, float))
        and bool((r.get("description") or "").strip())
        and bool((r.get("name") or "").strip())
    )


def flatten_features(r: dict, cap: int = 25) -> list[str]:
    out: list[str] = []
    for feat in r.get("features") or []:
        for t in feat.get("types") or []:
            if t and t.strip() and t.strip().lower() != "not applicable":
                out.append(t.strip())
    # de-dupe, preserve order, cap length
    seen, deduped = set(), []
    for t in out:
        if t not in seen:
            seen.add(t)
            deduped.append(t)
    return deduped[:cap]


def address_of(r: dict) -> dict:
    loc = r.get("location") or {}
    addr = loc.get("address") or {}
    return {
        "neighborhood": loc.get("neighborhood"),
        "city": addr.get("city") or loc.get("city"),
        "state": addr.get("state") or loc.get("state"),
    }


def trim(r: dict) -> dict:
    geo = r["geoLocation"]["coordinates"]  # [lon, lat]
    rating = r.get("rating") or {}
    addr = address_of(r)
    return {
        "id": r.get("_id"),
        "name": (r.get("name") or "").strip(),
        "description": (r.get("description") or "").strip(),
        "cuisines": r.get("cuisines") or [],
        "priceRange": r.get("priceRange"),
        "overallRating": rating.get("overallRating"),
        "numberOfRatings": rating.get("numberOfRatings"),
        "latitude": geo[1],
        "longitude": geo[0],
        "neighborhood": addr["neighborhood"],
        "city": addr["city"],
        "state": addr["state"],
        "phone": r.get("phone"),
        "websiteUrl": r.get("websiteUrl"),
        "features": flatten_features(r),
    }


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--size", type=int, default=1500)
    args = ap.parse_args()

    rng = random.Random(SEED)
    raw = json.loads(Path(args.source).read_text())
    pool = [r for r in raw if usable(r)]
    print(f"total={len(raw)} usable={len(pool)}")

    # densest metro (city, state)
    metro = Counter((address_of(r)["city"], address_of(r)["state"]) for r in pool)
    (top_city, top_state), top_n = metro.most_common(1)[0]
    print(f"densest metro: {top_city}, {top_state} ({top_n} restaurants)")

    top_pool = [r for r in pool if (address_of(r)["city"], address_of(r)["state"]) == (top_city, top_state)]
    rest_pool = [r for r in pool if (address_of(r)["city"], address_of(r)["state"]) != (top_city, top_state)]

    n_top = min(len(top_pool), max(args.size // 3, 400))
    n_rest = max(args.size - n_top, 0)
    picked = rng.sample(top_pool, n_top) + rng.sample(rest_pool, min(n_rest, len(rest_pool)))
    rng.shuffle(picked)

    sample = [trim(r) for r in picked]
    # centroid of the dense metro -> a good demo query point
    lat_c = sum(trim(r)["latitude"] for r in top_pool) / len(top_pool)
    lon_c = sum(trim(r)["longitude"] for r in top_pool) / len(top_pool)

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(sample, ensure_ascii=False, indent=1))
    print(f"wrote {len(sample)} restaurants -> {out} ({out.stat().st_size/1_000_000:.2f} MB)")
    print(f"suggested demo query: latitude={lat_c:.6f} longitude={lon_c:.6f}  (center of {top_city})")


if __name__ == "__main__":
    main()
