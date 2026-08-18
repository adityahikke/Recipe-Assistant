# Database Setup Guide

This guide will help you set up PostgreSQL database for the Food Recipe Recommendation Agent.

## Prerequisites

- PostgreSQL installed on your system
- Java 17 or higher
- Maven

## Step 1: Install PostgreSQL

### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

### macOS:
```bash
brew install postgresql
brew services start postgresql
```

### Windows:
Download and install from [postgresql.org](https://www.postgresql.org/download/windows/)

## Step 2: Start PostgreSQL Service

### Ubuntu/Debian:
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### macOS:
```bash
brew services start postgresql
```

### Windows:
PostgreSQL service should start automatically after installation.

## Step 3: Access PostgreSQL

Switch to the postgres user and access the PostgreSQL command line:

```bash
sudo -u postgres psql
```

## Step 4: Run Database Setup Script

In the PostgreSQL command line, run the setup script:

```sql
\i database_setup.sql
```

Or manually execute these commands:

```sql
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
```

## Step 5: Verify Setup

Test the connection:

```bash
psql -h localhost -U recipe_user -d recipe_assistant
```

When prompted, enter the password: `recipe_password`

## Step 6: Update Application Configuration

The application is already configured to use the database. The configuration is in `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/recipe_assistant
spring.datasource.username=recipe_user
spring.datasource.password=recipe_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate

# Schema is managed by Flyway (src/main/resources/db/migration/)
spring.flyway.baseline-on-migrate=true
```

## Step 7: Run the Application

Start the application:

```bash
mvn spring-boot:run
```

The application will automatically:
- Connect to the database
- Create tables based on the JPA entities
- Be ready to store and retrieve recipes

## Database Schema

The application will create the following tables:

### Users Table
- `id` (UUID, Primary Key)
- `email` (VARCHAR, Unique)
- `created_at` (TIMESTAMP)
- `last_login` (TIMESTAMP)

### Recipes Table
- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key to Users)
- `client_id` on users links library to device cookie (`ra_client_id`)
- `title`, `ingredients`, `instructions` (structured recipe JSON), `cuisine`, `dietary_restrictions`
- `content_hash` (dedupe), `created_at`, `updated_at`

### Collections
- `collections` — cookbook shelves (system: **My Recipes**, **Saved**; plus user-created)
- `collection_recipes` — many-to-many with ordering

## Features Enabled

- Persistent recipe library per device (PostgreSQL)
- Collection / cookbook UI
- Trial usage tracking (`trial_client_usage`)

Legacy tables `user_favorites` and `recipes.is_favorite` are unused; safe to drop manually if present.

## Troubleshooting

### Connection Issues
- Ensure PostgreSQL is running: `sudo systemctl status postgresql`
- Check if port 5432 is open: `netstat -an | grep 5432`
- Verify credentials in `application.properties`

### Permission Issues
- Ensure the `recipe_user` has proper privileges
- Check if the database exists: `\l` in psql
- Verify user permissions: `\du` in psql

### Table Creation Issues
- Check Hibernate logs for SQL errors
- Ensure UUID extension is enabled: `CREATE EXTENSION IF NOT EXISTS "uuid-ossp";`
- Verify JPA configuration in `application.properties`

## Next Steps

After database setup, you can:

1. **Deploy to Railway** — see `RAILWAY.md`
2. **Add user authentication** — cross-device library (optional)

## Security Notes

- Change default passwords in production
- Use environment variables for sensitive data
- Enable SSL for database connections
- Regular database backups
- Monitor database performance
