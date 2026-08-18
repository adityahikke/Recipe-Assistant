-- Remove legacy favorites artifacts from earlier iterations

DROP TABLE IF EXISTS user_favorites;

ALTER TABLE recipes DROP COLUMN IF EXISTS is_favorite;
