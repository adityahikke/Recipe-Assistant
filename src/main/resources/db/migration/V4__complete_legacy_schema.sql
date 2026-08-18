-- Complete brownfield alignment after V3 (constraints, NOT NULL, FKs, missing tables)

-- Hibernate @Column(unique=true) on users.client_id needs a constraint, not only an index
-- V1 greenfield creates a UNIQUE constraint; brownfield V3 may create a same-named index instead
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_client_id_key;
DROP INDEX IF EXISTS users_client_id_key;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey)
        WHERE t.relname = 'users'
          AND c.contype = 'u'
          AND a.attname = 'client_id'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_client_id_key UNIQUE (client_id);
    END IF;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE recipes ADD COLUMN IF NOT EXISTS ingredients TEXT;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS instructions TEXT;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE recipes SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE recipes SET title = 'Untitled' WHERE title IS NULL;
ALTER TABLE recipes ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE recipes ALTER COLUMN title SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_recipes_user_id ON recipes(user_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_schema = kcu.constraint_schema
         AND tc.constraint_name = kcu.constraint_name
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'recipes'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'user_id'
    ) THEN
        ALTER TABLE recipes
            ADD CONSTRAINT fk_recipes_user_id
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS collections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description TEXT,
    slug VARCHAR(120) NOT NULL,
    is_system_default BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_collections_user_slug UNIQUE (user_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_collections_user_id ON collections(user_id);

CREATE TABLE IF NOT EXISTS collection_recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_collection_recipes_pair UNIQUE (collection_id, recipe_id)
);

CREATE INDEX IF NOT EXISTS idx_collection_recipes_collection ON collection_recipes(collection_id);
CREATE INDEX IF NOT EXISTS idx_collection_recipes_recipe ON collection_recipes(recipe_id);

CREATE TABLE IF NOT EXISTS trial_client_usage (
    client_id VARCHAR(36) PRIMARY KEY,
    recipe_count INTEGER NOT NULL DEFAULT 0,
    ip_hash VARCHAR(64),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
