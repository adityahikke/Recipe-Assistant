# Deploying to Railway

## 1. Create the project

1. Go to [railway.app](https://railway.app) and create a new project.
2. **Deploy from GitHub** and connect this repository, or deploy with the Railway CLI.

## 2. Add PostgreSQL

1. In your Railway project, click **+ New** → **Database** → **PostgreSQL**.
2. Open the Postgres service → **Variables** and note `DATABASE_URL` (or the individual `PGHOST`, `PGUSER`, etc.).

Spring Boot does not read `DATABASE_URL` by default. Map it in your Railway **app service** variables:

| Variable | Value |
|----------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:PORT/railway` (use host/port/db from Railway Postgres) |
| `SPRING_DATASOURCE_USERNAME` | Postgres user from Railway |
| `SPRING_DATASOURCE_PASSWORD` | Postgres password from Railway |

Or set in Railway using variable references from the Postgres plugin if your template supports it.

## 3. Required environment variables (app service)

Open your **Java app service** → **Variables** → add:

| Variable | Required | Description |
|----------|----------|-------------|
| `OPENAI_API_KEY` | Yes (for free trial) | Shared demo key. **Never commit this.** Users get 5 recipes per **device** (cookie) without their own key. |
| `APP_ENCRYPTION_KEY` | Yes | Random string, **at least 32 characters**. Encrypts user API keys in session. Generate: `openssl rand -base64 32` |
| `APP_SESSION_COOKIE_SECURE` | Recommended | Set to `true` so session cookies are HTTPS-only. **Do not use `SERVER_SSL_ENABLED`** — Spring Boot maps that to `server.ssl.enabled` and the app will crash without a keystore. |
| `APP_CORS_ALLOWED_ORIGINS` | Recommended | Your public app URL, e.g. `https://ai-recipe-assistant-agent.up.railway.app` (comma-separated if multiple). |
| `SPRING_PROFILES_ACTIVE` | Optional | Set to `prod` explicitly, or rely on auto-detect when `RAILWAY_ENVIRONMENT` is set. |
| `SPRING_DATASOURCE_URL` | Yes | JDBC URL to Railway Postgres |
| `SPRING_DATASOURCE_USERNAME` | Yes | DB user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | DB password |

Mark `OPENAI_API_KEY`, `APP_ENCRYPTION_KEY`, and `SPRING_DATASOURCE_PASSWORD` as **secrets** in Railway.

### Optional

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_DEFAULT_KEY_MAX_RECIPES` | `5` | Free recipes per **device** (browser cookie) when using the shared key |

## 4. Build & start commands

In the app service **Settings**:

- **Build command:** `mvn -DskipTests clean package`
- **Start command:** `java -jar target/food-recipe-recommendation-agent-1.0.0.jar`

Confirm the JAR name in `pom.xml` `<artifactId>` if the start command fails.

## 5. Networking

1. App service → **Settings** → **Networking** → **Generate domain**.
2. Railway serves HTTPS at the edge; the app runs HTTP internally (`server.ssl.enabled=false` in the `prod` profile). Set `APP_SESSION_COOKIE_SECURE=true` for secure cookies. The `prod` profile activates automatically when Railway env vars are present, or set `SPRING_PROFILES_ACTIVE=prod`.

## 6. OpenAI dashboard (recommended)

On the **shared** `OPENAI_API_KEY` account:

- Set a **monthly usage limit**.
- Monitor usage; trial users share this key (5 recipes per device by default).

## 7. How keys and favorites work in production

| User action | Behavior |
|-------------|----------|
| No user key, trial remaining | Server uses `OPENAI_API_KEY`; counts per device cookie |
| No user key, trial used | Must add their own `sk-...` key in the UI |
| User sets own key | Their key is used; unlimited (subject to IP rate limit: 10 req/min) |
| Save favorite | Stored in Postgres **Saved** collection (device library) |

The default key is **never** sent to the browser.

## 8. Multiple Railway instances

Trial counters are stored in **PostgreSQL** (`trial_client_usage`), so multiple replicas share counts. Session-stored user API keys still require **sticky sessions** if you scale horizontally.

## 9. Verify after deploy

```bash
curl https://ai-recipe-assistant-agent.up.railway.app/api-key-status
```

Expect JSON with `defaultKeyAvailable: true` and `defaultTrialsRemaining: 5` when no user key is set.

Quick smoke checks:

- `GET /` — hero **What's in your fridge?**, assets `v=1.4.0`
- `POST /generate-recipe` with `{"ingredients":"car, temple"}` → **400** (validation; no trial credit)

## 10. Local development with the same behavior

```powershell
$env:OPENAI_API_KEY = "sk-your-demo-key"
$env:APP_ENCRYPTION_KEY = "local-dev-encryption-key-32chars!!"
mvn spring-boot:run
```

Without `OPENAI_API_KEY`, users must enter their own key (same as before).

## Troubleshooting: "SSL is enabled but no trust material is configured"

This happens if `SERVER_SSL_ENABLED=true` is set in Railway variables. **Delete that variable.** Railway terminates TLS; the JAR must not enable Tomcat SSL. Use `APP_SESSION_COOKIE_SECURE=true` instead, and ensure `SPRING_PROFILES_ACTIVE=prod` (or rely on Railway setting it).

## Troubleshooting: Flyway checksum mismatch on V4

The app runs `flyway.repair()` before `migrate()` on startup, which realigns checksums when a migration file changes on an already-migrated database (e.g. after the greenfield-safe V4 edit).

If startup still fails, repair manually in Railway Postgres → **Query**:

```sql
UPDATE flyway_schema_history SET checksum = -593969090 WHERE version = '4';
```

(`-593969090` is the resolved checksum for the current V4 on LF line endings.)
