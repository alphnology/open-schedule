# Troubleshooting

---

## Application won't start

### `UnsupportedClassVersionError`
The JRE version is lower than the compiled bytecode version.

```
java.lang.UnsupportedClassVersionError: ... has been compiled by a more recent version of the Java Runtime
```

**Fix:** Ensure the JRE matches the compiled version. The project requires Java 25.
```bash
java -version   # must be 25.x
```

### `Could not resolve placeholder`

```
Could not resolve placeholder 'APPLICATION_VARIABLE' in value '${APPLICATION_VARIABLE}'
```

A required environment variable is missing.

**Fix:** Check your `.env` file. Compare it against `.env.dist` — a variable may have been added since you copied it.

```bash
diff .env .env.dist
```

### Flyway migration fails

```
FlywayException: Found non-empty schema(s) ... without schema history table!
```

**Fix:** The database has data but no Flyway history. Run with `baseline-on-migrate=true` (already set in `application.yml`) or clean the database.

```
Caused by: org.flywaydb.core.api.exception.FlywayValidateException
```

The database has a migration that doesn't match the scripts. This usually means someone manually altered the schema.

**Fix for dev:** `docker compose down -v && docker compose up -d` (destroys and recreates the database).

---

## Database

### `Connection refused` to PostgreSQL

```
Connection to localhost:5432 refused.
```

PostgreSQL is not running or not ready yet.

```bash
docker compose ps postgres          # check status
docker compose logs postgres        # check for errors
docker compose up -d postgres       # start if stopped
```

Wait for the healthcheck to pass (up to 30s on first start).

### `FATAL: password authentication failed`

`DB_USER` or `DB_PASSWORD` mismatch between the app and PostgreSQL.

```bash
# Verify what credentials PostgreSQL was created with:
docker compose exec postgres psql -U postgres -c "\du"
```

---

## Storage

### Photos not showing / broken images

1. Check `STORAGE_PUBLIC_ENDPOINT` — this must be reachable from the browser, not just from the server.
2. Check signed URL expiry: if `STORAGE_SIGNED_URL_EXPIRY` is very low (< 60s), URLs may expire before the browser loads them.
3. Check SeaweedFS is running: `docker compose ps seaweedfs`

### Upload fails silently

Check the application logs for exceptions:
```bash
docker compose logs app-open-schedule | grep -i "Failed to upload\|storage"
```

Common causes:
- `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` incorrect
- Bucket does not exist and auto-creation failed (permissions issue)
- SeaweedFS master is not ready

---

## Email

### Emails not being sent

1. Confirm `EMAIL_SMTP_ENABLED=true` in your `.env`
2. Check application logs: `grep -i email` in the logs
3. If using Mailpit locally: ensure Mailpit container is running and check `http://localhost:8025`
4. If using Postal: verify `POSTAL_ENABLED=true` and the API key is valid

### `Authentication failed` (SMTP)

Wrong `EMAIL_SMTP_USERNAME` or `EMAIL_SMTP_PASSWORD`.

- **SendGrid:** username must be `apikey` (literal string), password is the API key
- **Mailjet:** username is the API key, password is the secret key

### `Connection timed out` (SMTP)

The server cannot reach `EMAIL_SMTP_HOST` on `EMAIL_SMTP_PORT`. Check firewall rules and that the host/port are correct.

### Emails going to spam

Your sending domain needs proper DNS records:
- **SPF** — authorizes your mail server
- **DKIM** — cryptographic signature
- **DMARC** — policy enforcement

All three must be set up with your DNS provider. SendGrid and Mailjet provide setup instructions in their dashboards.

---

## Vaadin / Frontend

### White screen after login

The Vaadin frontend bundle is not built for production. Run with the `production` profile:
```bash
./mvnw clean package -Pproduction
```

In development, run with the `dev` profile and let Vaadin compile the frontend on first start.

### Vaadin hot reload not working

Ensure the `dev` profile is active:
```bash
./mvnw spring-boot:run -Pdev
```
Or in IntelliJ: add `-Dspring.profiles.active=dev` to the run configuration VM options.

### `pnpm not found`

Vaadin requires pnpm for frontend builds. It is auto-installed by the Vaadin Maven plugin on first run. If it fails:

```bash
npm install -g pnpm
```

---

## Docker

### Port already in use

Another process is using port `51675`, `5432`, `8025`, or `8333`.

```bash
# Find what's using port 51675
lsof -i :51675
```

Either stop the conflicting process or change the port in `.env` / `docker-compose.yml`.

### Container keeps restarting

```bash
docker compose logs <service-name>
```

Read the last few lines for the root cause.

---

## Getting more help

If your issue is not covered here:

1. Search [GitHub Issues](https://github.com/alphnology/open-schedule/issues) — it may already be reported
2. Open a [GitHub Discussion](https://github.com/alphnology/open-schedule/discussions) with full logs
3. File a [bug report](https://github.com/alphnology/open-schedule/issues/new?template=bug_report.yml) with reproduction steps
