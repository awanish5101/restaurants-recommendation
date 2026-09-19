-- pgvector extension (embedding column is added in a later migration once the
-- embedding model/dimension is fixed).
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE restaurants (
    id                BIGINT PRIMARY KEY,
    name              TEXT NOT NULL,
    description       TEXT,
    cuisines          JSONB            NOT NULL DEFAULT '[]',
    features          JSONB            NOT NULL DEFAULT '[]',
    price_range       INTEGER,
    overall_rating    DOUBLE PRECISION,
    number_of_ratings INTEGER,
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    neighborhood      TEXT,
    city              TEXT,
    state             TEXT,
    phone             TEXT,
    website_url       TEXT
);

CREATE INDEX idx_restaurants_price ON restaurants (price_range);
CREATE INDEX idx_restaurants_city ON restaurants (city, state);

CREATE TABLE user_history (
    id               BIGSERIAL PRIMARY KEY,
    user_id          TEXT   NOT NULL,
    restaurant_id    BIGINT NOT NULL,
    interaction_type TEXT   NOT NULL,
    rating_given     DOUBLE PRECISION,
    interaction_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_history_user       ON user_history (user_id);
CREATE INDEX idx_user_history_restaurant ON user_history (restaurant_id);
