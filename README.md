<p align="center">
  <img src="assets/logo.png" alt="Open Schedule Logo" width="120"/>
</p>

<h1 align="center">Open Schedule</h1>

<p align="center">
  Conference schedule management with a public attendee experience and an authenticated administrative back office.
</p>

<p align="center">
  <a href="https://github.com/alphnology/open-schedule/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"></a>
  <img src="https://img.shields.io/badge/version-1.0.12-brightgreen.svg" alt="Version">
  <img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vaadin-25.1.6-blue.svg" alt="Vaadin">
</p>

---

## Overview

Open Schedule is a self-hostable event application built with Java 25, Spring Boot 4, and Vaadin 25. It combines:

- A public schedule and speaker experience
- A protected admin area for operating the event
- S3-compatible media storage for event, speaker, and news images
- Runtime-configurable outbound email with an admin UI
- Public workshop registration validated against alf.io
- PostgreSQL persistence with Flyway migrations

The current application model is centered around a single active event record. The admin event screen intentionally manages one event configuration, not a list of events.

---

## Current Capabilities

### Public experience

- Schedule browsing
- Speaker directory and speaker detail views
- News / announcements
- Public workshop registration via alf.io ticket validation
- Session rating for authenticated users
- Favorite sessions for authenticated users
- vCard / QR-based speaker sharing
- Report issue view
- About page

### Admin area

All admin views are protected with `ADMIN` role checks.

- Dashboard
- Event configuration
- Speaker management
- Session management
- Room management
- Track management
- Tag management
- News management
- Attendee management
- User management
- Mail settings
- Workshop registration settings and registration management

### Media and branding

- Speaker images stored in S3-compatible object storage
- News images stored in S3-compatible object storage
- Event image stored in S3-compatible object storage
- The drawer / sidebar logo uses the current event image when available
- Falls back to `assets/logo.png` when no event image exists
- Optional external CSS override file for customer-specific branding in production

### Email

- SMTP transport
- Provider presets for SMTP, SendGrid, and Mailjet
- Postal HTTP API transport
- Admin UI for runtime mail settings
- Optional encrypted secret persistence for mail credentials
- Test email action from the admin panel
- Sign-up welcome email
- Forgot-password flow using a temporary password emailed to the user

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Backend | Spring Boot 4.0.6 |
| UI | Vaadin Flow 25.1.6 |
| Security | Spring Security + Vaadin Security |
| Database | PostgreSQL 18 + Flyway |
| Storage | S3-compatible object storage via MinIO Java client |
| Email | Angus Mail + Postal HTTP API |
| Templates | Thymeleaf |
| Charts | ApexCharts |
| Build | Maven |
| Containers | Docker / Docker Compose |

---

## Running the Project

### Recommended local development

This is the most accurate local workflow for the current repository state.

1. Copy environment defaults:

```bash
cp .env.dist .env
```

2. Start local infrastructure:

```bash
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5433`
- Mailpit SMTP on `localhost:1026`
- Mailpit UI on `http://localhost:8026`
- MinIO S3 API on `http://localhost:9000`
- MinIO Console on `http://localhost:9001`

3. Run the application:

```bash
./mvnw spring-boot:run
```

4. Open:

- App: [http://localhost:51675](http://localhost:51675)
- Mailpit UI: [http://localhost:8026](http://localhost:8026)
- MinIO Console: [http://localhost:9001](http://localhost:9001)

### Production-style build

```bash
make build
```

This produces the packaged JAR in `target/`.

### Docker image build

```bash
make build-image
```

For AMD64:

```bash
make build-image-amd64
```

---

## Configuration Highlights

All configuration is environment-driven. The main areas are:

- Database: `DB_*`
- Application URLs and naming: `APP_*`, `EVENT_*`
- Storage: `STORAGE_*`
- Email: `EMAIL_*`, `POSTAL_*`
- Optional external branding CSS: `BRANDING_EXTERNAL_CSS_PATH`

Use these docs for full details:

- [docs/configuration.md](docs/configuration.md)
- [docs/email.md](docs/email.md)
- [docs/storage.md](docs/storage.md)
- [docs/deployment.md](docs/deployment.md)
- [docs/workshop-registration.md](docs/workshop-registration.md)

---

## Workshop Registration

Open Schedule includes a workshop-registration flow for events that sell tickets through alf.io.

What it supports:

- Public route: `/workshop-registration`
- Ticket validation against the alf.io admin reservation API
- Workshop discovery from sessions marked as `SessionType.W`
- Capacity enforcement from `room.capacity`
- Duplicate prevention with one active registration per ticket
- Admin management to search, move, and delete registrations

Operational notes:

- The attendee must enter the ticket PDF value shown as `Info. del pedido`
- The admin configures the alf.io base URL, event slug, and bearer token from `Admin -> Workshop registration`
- The alf.io token is encrypted before being stored in the database
- Encrypted UI secret persistence requires:

```bash
APP_SECRETS_MASTER_KEY=replace-with-a-strong-random-secret
APP_SECRETS_ALLOW_UI_PERSISTENCE=true
```

---

## External Branding CSS

The bundled theme is always the default. You can optionally load a CSS file from disk to override visual tokens without rebuilding the JAR.

Example:

```bash
cp branding-overrides.example.css branding-overrides.css
export BRANDING_EXTERNAL_CSS_PATH=./branding-overrides.css
```

How it works:

- If `BRANDING_EXTERNAL_CSS_PATH` is empty, only the internal theme is used
- If the file exists, the app injects it after the bundled theme
- It is served from `/branding-overrides/<filename>`

Example local URL:

- [http://localhost:51675/branding-overrides/branding-overrides.css](http://localhost:51675/branding-overrides/branding-overrides.css)

---

## Storage Behavior

Open Schedule currently uses object storage for:

- Event header image
- Speaker profile photos
- News images

The application targets S3-compatible APIs and works well with MinIO for development. Other compatible providers can be used as long as the endpoint and credentials are valid.

When the current event has an uploaded image, that image is shown in the main layout drawer. If not, the app falls back to `assets/logo.png`.

---

## Authentication and Access

Public routes are available for schedule browsing and related attendee flows.

Administrative routes require authenticated users with `ADMIN` role.

Important current behaviors:

- Sign-up is publicly accessible
- Forgot password sends a temporary password by email
- Session rating and favorites require a logged-in user session
- Mail settings are admin-only

---

## Repository Structure

| Path | Purpose |
|------|---------|
| `src/main/java/com/alphnology/views` | Vaadin views, including public and admin UI |
| `src/main/java/com/alphnology/services` | Application services |
| `src/main/java/com/alphnology/services/email` | Mail settings, sending, provider abstraction |
| `src/main/java/com/alphnology/infrastructure/storage` | S3-compatible object storage integration |
| `src/main/resources/db/migration` | Flyway migrations |
| `src/main/frontend/themes/open-schedule` | Theme and CSS |
| `docs/` | Project documentation |

---

## Notable Current Features

- Single-event admin form with image upload
- Admin mail settings with provider switching and test email
- Postal support via HTTP API
- External branding CSS loaded from filesystem
- Responsive admin shell with collapsible sidebar
- Public speaker vCard generation
- QR export for speakers from admin

---

## Documentation

| Guide | Description |
|-------|-------------|
| [docs/getting-started.md](docs/getting-started.md) | Local setup and development workflow |
| [docs/configuration.md](docs/configuration.md) | Environment variables and configuration reference |
| [docs/email.md](docs/email.md) | SMTP, SendGrid, Mailjet, Postal, and admin mail settings |
| [docs/storage.md](docs/storage.md) | S3-compatible storage configuration |
| [docs/deployment.md](docs/deployment.md) | Production deployment notes |
| [docs/architecture.md](docs/architecture.md) | Application structure and technical overview |
| [docs/contributing.md](docs/contributing.md) | Contribution workflow |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Common issues and fixes |

---

## Notes

- `docker-compose.yml` is the best local-dev compose entrypoint today because it includes PostgreSQL, Mailpit, and MinIO.
- The app depends on valid S3-compatible storage configuration for image upload features.
- The current password recovery flow is temporary-password based, not magic-link based.
- The branding override mechanism is optional and safe to leave disabled.

---

## License

MIT — see [LICENSE](LICENSE).
