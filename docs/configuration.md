# Configuration Reference

All configuration is done through environment variables. Copy `.env.dist` to `.env` and set the values for your environment.

> **Security:** Never commit `.env` to version control. Add it to `.gitignore`.

---

## Server

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `51675` | HTTP port the application listens on |

---

## Database

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `DB_HOST` | `localhost` | Yes | PostgreSQL hostname |
| `DB_PORT` | `5432` | Yes | PostgreSQL port |
| `DB_NAME` | `open-schedule` | Yes | Database name |
| `DB_USER` | `postgres` | Yes | Database username |
| `DB_PASSWORD` | `postgres` | **Yes** | Database password — change in production |

---

## Object Storage (S3-compatible)

Open Schedule uses any S3-compatible object storage backend for speaker photos and attachments.

| Variable | Default | Description |
|----------|---------|-------------|
| `STORAGE_ENDPOINT` | `http://localhost:8333` | S3 API endpoint (SeaweedFS default for dev) |
| `STORAGE_ACCESS_KEY` | `any` | Access key (SeaweedFS unauthenticated in dev) |
| `STORAGE_SECRET_KEY` | `any` | Secret key |
| `STORAGE_BUCKET` | `open-schedule` | Bucket / collection name |
| `STORAGE_PUBLIC_ENDPOINT` | _(same as STORAGE_ENDPOINT)_ | Public URL for signed URLs. Set this when storage is behind a separate public hostname |
| `STORAGE_SIGNED_URL_EXPIRY` | `3600` | Signed URL expiry in seconds |

See [storage.md](storage.md) for provider-specific examples.

---

## Email

### General (shared across all providers)

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_FROM_ADDRESS` | _(none)_ | Sender email address |
| `EMAIL_FROM_NAME` | `Open Schedule` | Sender display name |

### SMTP Provider

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_SMTP_ENABLED` | `true` | Set to `false` to disable all outbound email |
| `EMAIL_SMTP_HOST` | _(none)_ | SMTP server hostname |
| `EMAIL_SMTP_PORT` | `587` | SMTP port (587 = STARTTLS, 465 = SSL) |
| `EMAIL_SMTP_USERNAME` | _(none)_ | SMTP authentication username |
| `EMAIL_SMTP_PASSWORD` | _(none)_ | SMTP authentication password |
| `EMAIL_SMTP_SSL_TRUST` | _(none)_ | Set to SMTP hostname only when using a self-signed certificate. Leave blank for public providers |

### Postal HTTP API Provider

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTAL_ENABLED` | `false` | Set to `true` to use Postal instead of SMTP |
| `POSTAL_BASE_URL` | _(none)_ | Postal server base URL, e.g. `https://postal.yourdomain.com` |
| `POSTAL_API_KEY` | _(none)_ | Postal server API key |

> When `POSTAL_ENABLED=true`, Postal takes precedence over SMTP automatically. Set `EMAIL_SMTP_ENABLED=false` to avoid loading both providers.

See [email.md](email.md) for complete provider setup guides.

---

## Application

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_URL` | `https://alphnology.com` | Public base URL of the application — used in email links |
| `APP_NAME` | `Open Schedule` | Application display name |

---

## Event

| Variable | Default | Description |
|----------|---------|-------------|
| `EVENT_WEBSITE` | `https://alphnology.com` | URL shown in the "Event Website" nav link |
| `EVENT_SCHEDULE` | `https://alphnology.com` | URL for the schedule link |

---

## GitHub Integration _(optional)_

Used by the "Report a Bug" feature in the app. Leave blank to disable it.

| Variable | Default | Description |
|----------|---------|-------------|
| `GIT_HUB_API_TOKEN` | _(none)_ | GitHub Personal Access Token with `repo` scope |
| `GIT_HUB_REPOSITORY_NAME` | `alphnology/open-schedule` | Repository to open issues against |

---

## Date & Time Formatting

| Variable | Default | Description |
|----------|---------|-------------|
| `FORMATTER_DATE` | `MM-dd-yyyy` | Date display format |
| `FORMATTER_TIME` | `HH:mm` | Time display format (24h) |
| `FORMATTER_TIME_12` | `hh:mm a` | Time display format (12h) |
| `FORMATTER_DATE_TIME` | `MM-dd-yyyy HH:mm` | Date + time display format |
| `FORMATTER_DATE_TIME_12` | `MM-dd-yyyy hh:mm a` | Date + time display format (12h) |

---

## Miscellaneous

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIFICATION_TIME` | `3000` | Notification auto-dismiss time in milliseconds |

---

## Production checklist

```
[ ] DB_PASSWORD is a strong, unique password
[ ] STORAGE_ACCESS_KEY and STORAGE_SECRET_KEY are not default values
[ ] EMAIL_SMTP_PASSWORD / POSTAL_API_KEY are rotated secrets
[ ] GIT_HUB_API_TOKEN has minimum required scope
[ ] APP_URL matches your actual public domain
[ ] EMAIL_FROM_ADDRESS is a verified sender domain
[ ] .env is in .gitignore and never committed
```
