-- Backfill columns missing from pre-Flyway Hibernate schemas (Railway brownfield)

ALTER TABLE users ADD COLUMN IF NOT EXISTS client_id VARCHAR(36);
CREATE UNIQUE INDEX IF NOT EXISTS users_client_id_key ON users(client_id);

ALTER TABLE recipes ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS dietary_restrictions TEXT;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS prep_time INTEGER;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS cook_time INTEGER;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS servings INTEGER;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS cuisine VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_recipes_user_content_hash ON recipes(user_id, content_hash);
