-- Database Setup Script for Recipe Assistant
-- Run this script as a PostgreSQL superuser (e.g., postgres)

-- Create database
CREATE DATABASE recipe_assistant;

-- Create user
CREATE USER recipe_user WITH PASSWORD 'recipe_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE recipe_assistant TO recipe_user;

-- Connect to the database
\c recipe_assistant;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant additional privileges
GRANT ALL ON SCHEMA public TO recipe_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO recipe_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO recipe_user;

-- Create indexes for better performance (optional - Hibernate will create tables)
-- These will be created automatically by Hibernate, but you can add custom indexes here

-- Full-text search index for recipes (will be created by Hibernate)
-- CREATE INDEX recipe_search_idx ON recipes USING GIN(to_tsvector('english', title || ' ' || instructions));

-- Index for user favorites (will be created by Hibernate)
-- CREATE INDEX user_favorites_user_idx ON user_favorites(user_id);
-- CREATE INDEX user_favorites_recipe_idx ON user_favorites(recipe_id);

-- Verify setup
SELECT 'Database setup completed successfully!' as status;
