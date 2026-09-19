-- Semantic embedding for each restaurant (gemini-embedding-001, truncated to 768 dims).
ALTER TABLE restaurants ADD COLUMN embedding vector(768);

-- Approximate-nearest-neighbour index for cosine similarity search.
CREATE INDEX idx_restaurants_embedding
    ON restaurants USING hnsw (embedding vector_cosine_ops);
