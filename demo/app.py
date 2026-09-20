import time
import requests
import streamlit as st
import folium
from streamlit_folium import st_folium

st.set_page_config(
    page_title="Restaurant Recommender",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom Styling for production polish
st.markdown("""
<style>
    .main-header {
        font-size: 2.2rem;
        font-weight: 700;
        margin-bottom: 0.2rem;
    }
    .sub-header {
        font-size: 1.05rem;
        color: #555;
        margin-bottom: 1.5rem;
    }
    .rec-card {
        background-color: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 1.25rem;
        margin-bottom: 1.25rem;
        box-shadow: 0 2px 4px rgba(0,0,0,0.04);
        transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .rec-card:hover {
        box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    .rec-title {
        font-size: 1.3rem;
        font-weight: 700;
        color: #1a202c;
    }
    .cuisine-badge {
        display: inline-block;
        background-color: #edf2f7;
        color: #4a5568;
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 0.8rem;
        font-weight: 600;
        margin-right: 6px;
        margin-bottom: 6px;
    }
    .price-badge {
        display: inline-block;
        background-color: #e6fffa;
        color: #234e52;
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 0.8rem;
        font-weight: 600;
        margin-right: 6px;
    }
    .rationale-box {
        background-color: #f7fafc;
        border-left: 4px solid #3182ce;
        padding: 0.75rem 1rem;
        border-radius: 0 8px 8px 0;
        margin-top: 0.75rem;
        font-size: 0.95rem;
        line-height: 1.45;
        color: #2d3748;
    }
    .metric-pill {
        font-weight: 600;
        font-size: 0.9rem;
    }
</style>
""", unsafe_allow_html=True)

# ----------------- SIDEBAR -----------------
with st.sidebar:
    st.image("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop&q=60", use_container_width=True)
    st.title("Recommender Config")

    backend_url = st.text_input("Backend Base URL", value="http://localhost:8080", help="Spring Boot API base URL")

    # Health check
    is_healthy = False
    try:
        r = requests.get(f"{backend_url}/actuator/health", timeout=2)
        if r.status_code == 200 and r.json().get("status") == "UP":
            is_healthy = True
            st.success("Backend Connected (Healthy)")
        else:
            st.warning(f"Backend Responded: {r.status_code}")
    except Exception:
        st.error("Backend Offline (Start Spring Boot on port 8080)")

    st.markdown("---")
    st.subheader("Diner Location")
    
    preset = st.selectbox(
        "Location Presets",
        ["Times Square, NYC", "SoHo, NYC", "Upper West Side, NYC", "DUMBO, Brooklyn", "Custom Coordinates"]
    )

    preset_coords = {
        "Times Square, NYC": (40.7580, -73.9855),
        "SoHo, NYC": (40.7233, -74.0030),
        "Upper West Side, NYC": (40.7870, -73.9754),
        "DUMBO, Brooklyn": (40.7033, -73.9881),
    }

    if preset in preset_coords:
        lat, lon = preset_coords[preset]
    else:
        lat = st.number_input("Latitude", value=40.7580, format="%.5f")
        lon = st.number_input("Longitude", value=-73.9855, format="%.5f")

    st.markdown("---")
    st.subheader("Diner Preferences")

    vibe_quickpicks = [
        "Cozy Italian pasta & wine",
        "Authentic Japanese sushi & omakase",
        "Spicy street tacos & margaritas",
        "Artisanal sourdough bakery & espresso",
        "Trendy rooftop bar & craft cocktails",
        "Gourmet smash burger & craft beer"
    ]
    
    selected_quickpick = st.selectbox("Quick Vibe Templates", ["(Custom Query)"] + vibe_quickpicks)
    
    default_cuisine = selected_quickpick if selected_quickpick != "(Custom Query)" else "Cozy Italian pasta"
    cuisine_input = st.text_input("Cuisine or Vibe Description", value=default_cuisine, help="Semantic prompt embedded for pgvector cosine match")

    max_dist = st.slider("Max Distance (km)", min_value=1.0, max_value=30.0, value=10.0, step=0.5)
    min_rating = st.slider("Minimum Rating (Stars)", min_value=0.0, max_value=5.0, value=3.5, step=0.5)

    price_labels = {0: "Any", 1: "$ (Budget)", 2: "$$ (Moderate)", 3: "$$$ (Upscale)", 4: "$$$$ (Fine Dining)"}
    price_val = st.select_slider("Preferred Price", options=list(price_labels.keys()), format_func=lambda x: price_labels[x], value=2)

    prioritize_rating = st.checkbox("Prioritize Rating Weight", value=True)

    st.markdown("---")
    st.markdown("### Developer Links")
    st.markdown(f"- [Swagger UI Documentation]({backend_url}/swagger-ui/index.html)")
    st.markdown(f"- [Spring Boot Actuator Health]({backend_url}/actuator/health)")
    st.markdown(f"- [OpenAPI JSON Spec]({backend_url}/v3/api-docs)")


# ----------------- MAIN UI -----------------
st.markdown('<div class="main-header">Restaurant Recommendation Engine</div>', unsafe_allow_html=True)
st.markdown(
    '<div class="sub-header"><b>Architecture:</b> Spring Boot 3 + PostgreSQL/pgvector HNSW Cosine Search + Spring AI / Gemini LLM Justifications + Graceful Rule Fallback</div>',
    unsafe_allow_html=True
)

col_btn, col_info = st.columns([1, 4])
with col_btn:
    search_clicked = st.button("Find Recommendations", type="primary", use_container_width=True)

if "results" not in st.session_state:
    st.session_state["results"] = None
if "latency_ms" not in st.session_state:
    st.session_state["latency_ms"] = None
if "query_meta" not in st.session_state:
    st.session_state["query_meta"] = None

if search_clicked:
    if not is_healthy:
        st.error("Backend is not reachable. Please start the Spring Boot app.")
    else:
        req_body = {
            "preferredCuisine": cuisine_input,
            "maxDistanceInKm": max_dist,
            "prioritizeRating": prioritize_rating,
            "preferredPriceRange": price_val,
            "minimumRating": int(min_rating)
        }
        api_endpoint = f"{backend_url}/api/restaurants/recommend?latitude={lat}&longitude={lon}"
        
        t0 = time.perf_counter()
        with st.spinner("Executing pgvector hybrid search & generating rationale..."):
            try:
                resp = requests.post(api_endpoint, json=req_body, timeout=25)
                elapsed_ms = (time.perf_counter() - t0) * 1000.0
                if resp.status_code == 200:
                    st.session_state["results"] = resp.json()
                    st.session_state["latency_ms"] = elapsed_ms
                    st.session_state["query_meta"] = {
                        "lat": lat,
                        "lon": lon,
                        "cuisine": cuisine_input,
                        "max_dist": max_dist
                    }
                else:
                    st.error(f"API Error {resp.status_code}: {resp.text}")
                    st.session_state["results"] = None
            except Exception as e:
                st.error(f"Failed to communicate with API: {e}")
                st.session_state["results"] = None

# Render results
results = st.session_state.get("results")
latency_ms = st.session_state.get("latency_ms")
meta = st.session_state.get("query_meta")

if results is not None:
    if len(results) == 0:
        st.info("No restaurants matched your constraints. Try expanding your search distance or relaxing the minimum rating.")
    else:
        # Top KPI Metrics Row
        m1, m2, m3, m4 = st.columns(4)
        m1.metric("Recommendations Found", len(results))
        closest_dist = min((r.get("distance") or r.get("distanceInKm") or 0.0) for r in results)
        m2.metric("Closest Distance", f"{closest_dist:.2f} km")
        top_rating = max((r.get("overallRating") or 0.0) for r in results)
        m3.metric("Top Rating", f"{top_rating:.1f} / 5.0")
        m4.metric("RAG Query Latency", f"{latency_ms:.1f} ms")

        # Map & List Layout
        tab_list, tab_map, tab_diag = st.tabs(["Recommended Restaurants", "Geographic Map", "System Diagnostics"])

        with tab_map:
            # Create Folium Map
            center_lat = meta["lat"] if meta else lat
            center_lon = meta["lon"] if meta else lon
            m = folium.Map(location=[center_lat, center_lon], zoom_start=13, tiles="CartoDB positron")

            # Diner Marker
            folium.Marker(
                [center_lat, center_lon],
                popup="<b>Your Location (Diner)</b>",
                tooltip="Diner Location",
                icon=folium.Icon(color="blue", icon="user", prefix="fa")
            ).add_to(m)

            # Search Radius Circle
            if meta and meta.get("max_dist"):
                folium.Circle(
                    radius=meta["max_dist"] * 1000,
                    location=[center_lat, center_lon],
                    color="#3182ce",
                    fill=True,
                    fill_opacity=0.08,
                    weight=1.5
                ).add_to(m)

            # Restaurant Markers
            for rank, r in enumerate(results, 1):
                r_lat = r.get("latitude")
                r_lon = r.get("longitude")
                if r_lat is not None and r_lon is not None:
                    name = r.get("restaurantName", "Unknown")
                    rating = r.get("overallRating", "N/A")
                    cuisines = ", ".join(r.get("cuisines", []))
                    rationale = r.get("justification", "")
                    dist_val = r.get('distance') or r.get('distanceInKm', 0)
                    popup_html = f"""
                    <div style="font-family: sans-serif; min-width: 180px;">
                        <b style="font-size: 1.05rem;">#{rank} {name}</b><br/>
                        <span>Rating: {rating} | Distance: {dist_val:.2f} km</span><br/>
                        <span style="color: #666; font-size: 0.85rem;">{cuisines}</span><br/><br/>
                        <i>"{rationale}"</i>
                    </div>
                    """
                    folium.Marker(
                        [r_lat, r_lon],
                        popup=folium.Popup(popup_html, max_width=300),
                        tooltip=f"#{rank} {name} ({rating})",
                        icon=folium.Icon(color="red", icon="cutlery", prefix="fa")
                    ).add_to(m)

            st_folium(m, width="100%", height=500)

        with tab_list:
            for rank, rec in enumerate(results, 1):
                name = rec.get("restaurantName", "Restaurant")
                rating = rec.get("overallRating", 0.0)
                distance = rec.get("distance") or rec.get("distanceInKm") or 0.0
                price = rec.get("priceRange", 2)
                price_str = "$" * max(1, price)
                cuisines = rec.get("cuisines", [])
                justification = rec.get("justification", "")
                r_id = rec.get("id") or rec.get("restaurantId") or rank

                # Card HTML container
                badge_html = "".join([f'<span class="cuisine-badge">{c}</span>' for c in cuisines])
                
                st.markdown(f"""
                <div class="rec-card">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                        <div>
                            <span style="font-size: 1.1rem; font-weight: 700; color: #3182ce;">#{rank}</span>
                            <span class="rec-title"> {name}</span>
                            <div style="margin-top: 6px;">
                                <span class="price-badge">{price_str}</span>
                                {badge_html}
                            </div>
                        </div>
                        <div style="text-align: right;">
                            <span style="font-size: 1.1rem; font-weight: 700; color: #2d3748;">Rating: {rating:.1f}</span><br/>
                            <span style="color: #718096; font-size: 0.85rem;">{distance:.2f} km away</span>
                        </div>
                    </div>
                    <div class="rationale-box">
                        <b>Recommendation Rationale:</b> {justification}
                    </div>
                </div>
                """, unsafe_allow_html=True)

                # Action row for logging visit and ratings
                col_v, col_r, col_s = st.columns([1.5, 1.5, 4])
                with col_v:
                    if st.button(f"Log Visit", key=f"visit_{r_id}"):
                        try:
                            v_resp = requests.post(f"{backend_url}/history/visit?userId=demo_user&restaurantId={r_id}")
                            if v_resp.status_code == 200:
                                st.toast(f"Visit logged for {name}")
                            else:
                                st.error(f"Error {v_resp.status_code}")
                        except Exception as ex:
                            st.error(str(ex))

                with col_r:
                    user_score = st.selectbox("Rating", [5.0, 4.0, 3.0, 2.0, 1.0], key=f"score_{r_id}", label_visibility="collapsed")
                with col_s:
                    if st.button(f"Submit Rating", key=f"rate_{r_id}"):
                        try:
                            r_resp = requests.post(f"{backend_url}/history/rate?userId=demo_user&restaurantId={r_id}&rating={user_score}")
                            if r_resp.status_code == 200:
                                st.toast(f"Rated {name} {user_score} stars")
                            else:
                                st.error(f"Error {r_resp.status_code}")
                        except Exception as ex:
                            st.error(str(ex))

        with tab_diag:
            st.subheader("Raw API Response")
            st.json(results)
            st.subheader("Query Context Sent")
            st.json({
                "latitude": meta["lat"] if meta else lat,
                "longitude": meta["lon"] if meta else lon,
                "body": {
                    "preferredCuisine": cuisine_input,
                    "maxDistanceInKm": max_dist,
                    "prioritizeRating": prioritize_rating,
                    "preferredPriceRange": price_val,
                    "minimumRating": int(min_rating)
                }
            })
else:
    st.info("Set your preferences in the sidebar and click **'Find Recommendations'** to begin exploring.")
