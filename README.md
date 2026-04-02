<p align="center">
  <img src="assets/logo.png" alt="Open Schedule Logo" width="120"/>
</p>

<h1 align="center">Open Schedule</h1>

<p align="center">
  Modern conference schedule management — open source, self-hostable, production-ready.
</p>

<p align="center">
  <a href="https://github.com/alphnology/open-schedule/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"></a>
  <img src="https://img.shields.io/badge/version-1.0.11-brightgreen.svg" alt="Version">
  <img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vaadin-25-blue.svg" alt="Vaadin">
  <a href="https://github.com/alphnology/open-schedule/actions/workflows/ci.yml"><img src="https://github.com/alphnology/open-schedule/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
</p>

---

## What is Open Schedule?

Open Schedule is a free, self-hostable web application for managing conference and event agendas. Built with Java 25, Spring Boot 4, and Vaadin 25, it gives organizers a powerful admin interface and attendees a clean, interactive schedule experience — all in a single deployable JAR.

Originally built for [JconfDominicana 2025](https://jconfdominicana.org/), it is now a general-purpose platform for any event format.

> **Quick look:** sessions, speakers, rooms, tracks, live ratings, favorites, news, QR codes, vCards, PWA support, S3-compatible photo storage, and multi-provider email configurable from environment variables and an admin UI.

---

## Features

| Category | Features |
|----------|----------|
| **Admin** | Session CRUD, speaker management, room & track organization, tag system, attendee tracking, news/announcements, mail settings |
| **Public** | Interactive schedule, speaker profiles, session ratings, favorites, QR share, vCard download |
| **Infrastructure** | PostgreSQL, Flyway migrations, S3-compatible storage (SeaweedFS / MinIO / AWS S3), multi-provider email with admin-managed runtime settings |
| **Platform** | Docker deployment, Traefik reverse proxy, Let's Encrypt SSL, PWA, dark mode |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.x |
| UI | Vaadin 25 (server-side) |
| Database | PostgreSQL 15 + Flyway |
| Storage | SeaweedFS S3 API (MinIO SDK) |
| Email | Jakarta Mail · Postal HTTP API |
| Proxy | Traefik v3 + Let's Encrypt |
| Container | Docker + Docker Compose |

---

## Quick Start

### Option A — Run with Docker (self-hosting)

```bash
git clone https://github.com/alphnology/open-schedule.git
cd open-schedule

cp .env.dist .env
# Edit .env — set DB_PASSWORD, EMAIL_*, STORAGE_*, EVENT_WEBSITE at minimum

docker compose -f docker-compose-dev.yml up -d
```

Open `http://localhost:51675`

### Option B — Local development (IDE)

```bash
git clone https://github.com/alphnology/open-schedule.git
cd open-schedule

cp .env.dist .env            # Uses Mailpit + SeaweedFS defaults — no edits needed for dev

# Start infrastructure services
docker compose up -d

# Run the application (hot reload enabled)
./mvnw spring-boot:run -Pdev
```

**Service URLs:**

| Service | URL |
|---------|-----|
| Application | http://localhost:51675 |
| Mailpit (email catcher) | http://localhost:8025 |
| SeaweedFS S3 API | http://localhost:8333 |

---

## Documentation

| Guide | Description |
|-------|-------------|
| [Getting Started](docs/getting-started.md) | Local dev setup, prerequisites, IDE workflow |
| [Configuration](docs/configuration.md) | All environment variables with defaults and examples |
| [Email Setup](docs/email.md) | SMTP, SendGrid, Mailjet, Postal — provider configuration guide plus admin UI workflow |
| [Storage Setup](docs/storage.md) | SeaweedFS, MinIO, AWS S3 — object storage configuration |
| [Deployment](docs/deployment.md) | Production deployment with Docker Compose and Traefik |
| [Architecture](docs/architecture.md) | System design, layers, data model, security |
| [Contributing](docs/contributing.md) | How to contribute, code style, PR workflow |
| [Troubleshooting](docs/troubleshooting.md) | Common issues and solutions |

---

## Configuration at a Glance

All configuration is done through environment variables. Copy `.env.dist` to `.env` and fill in your values.

```bash
# Minimum required for production
DB_PASSWORD=your-secure-password
STORAGE_ENDPOINT=http://your-seaweedfs:8333
EMAIL_PROVIDER_TYPE=SENDGRID
EMAIL_SMTP_HOST=smtp.sendgrid.net
EMAIL_SMTP_PASSWORD=SG.your-api-key
EMAIL_FROM_ADDRESS=no-reply@yourconference.com
EVENT_WEBSITE=https://yourconference.com
APP_URL=https://schedule.yourconference.com
```

See [docs/configuration.md](docs/configuration.md) for the full reference.

---

## Contributing

We welcome contributions of all kinds — bug reports, feature requests, documentation improvements, and code.

1. **Fork** the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit with [Conventional Commits](https://www.conventionalcommits.org/): `git commit -m "feat: add session export"`
4. Push and open a Pull Request against `main`

See [docs/contributing.md](docs/contributing.md) for the full guide including setup, code style, and review expectations.

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

<p align="center">
  Built and maintained by <a href="https://alphnology.com">Alphnology</a>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/2148a45c-c922-4e51-8f96-ca492409f7c1" alt="Alphnology" width="160"/>
</p>
