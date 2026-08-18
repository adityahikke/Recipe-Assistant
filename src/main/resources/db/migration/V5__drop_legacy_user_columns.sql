-- Remove legacy columns on brownfield tables that block inserts but are not mapped by JPA.

DO $$
DECLARE
    col RECORD;
BEGIN
    FOR col IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name NOT IN ('id', 'email', 'created_at', 'last_login', 'client_id')
    LOOP
        EXECUTE format('ALTER TABLE users DROP COLUMN IF EXISTS %I', col.column_name);
    END LOOP;
END $$;

DO $$
DECLARE
    col RECORD;
BEGIN
    FOR col IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'recipes'
          AND column_name NOT IN (
              'id', 'user_id', 'title', 'ingredients', 'instructions', 'cuisine',
              'dietary_restrictions', 'prep_time', 'cook_time', 'servings',
              'content_hash', 'created_at', 'updated_at'
          )
    LOOP
        EXECUTE format('ALTER TABLE recipes DROP COLUMN IF EXISTS %I', col.column_name);
    END LOOP;
END $$;
