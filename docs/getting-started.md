# Getting Started

This guide gets you from zero to a running Open Schedule instance in under 10 minutes.

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| Java | 25 | [Eclipse Temurin](https://adoptium.net/) recommended |
| Maven | 3.9+ | Or use the included `./mvnw` wrapper |
| Docker | 24+ | With Docker Compose v2 |
| Git | any | |

---

## 1. Clone the repository

```bash
git clone https://github.com/alphnology/open-schedule.git
cd open-schedule
```

---

## 2. Configure the environment

```bash
cp .env.dist .env
```

For local development the defaults in `.env.dist` work out of the box — Mailpit handles email and SeaweedFS handles storage with no credentials required. You do not need to edit `.env` to start developing.

See [configuration.md](configuration.md) for the full variable reference.

---

## 3. Start infrastructure services

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 15** on port `5432`
- **Mailpit** on ports `1025` (SMTP) and `8025` (web UI)
- **SeaweedFS** on port `8333` (S3 API)

Wait ~10 seconds for PostgreSQL to be ready. Check status with:

```bash
docker compose ps
```

---

## 4. Run the application

### Option A — Maven (recommended for dev)

```bash
./mvnw spring-boot:run -Pdev
```

Vaadin hot-reload is active. Frontend and backend changes are reflected without restarting.

### Option B — IDE

Open the project in IntelliJ IDEA or VS Code and run `Application.java` with the `dev` Spring profile active:

```
-Dspring.profiles.active=dev
```

---

## 5. Access the application

| URL | Description |
|-----|-------------|
| `http://localhost:51675` | Application |
| `http://localhost:8025` | Mailpit — all outbound emails appear here |
| `http://localhost:8333` | SeaweedFS S3 API |

---

## Default credentials

The `dev` profile loads seed data from `src/main/resources/db/migration/dev/V9.9.9__dev_data.sql`.

Check that file for the default admin username and password. It is safe to use these locally; **never deploy dev seed data to production**.

---

## Useful commands

```bash
# Stop all services
docker compose down

# Stop services and remove volumes (full reset)
docker compose down -v

# View application logs (when running via docker-compose-dev.yml)
docker compose logs -f app-open-schedule

# Build production JAR
./mvnw clean package -DskipTests -Pproduction

# Run all tests
./mvnw test
```

---

## Next steps

- [Configuration reference](configuration.md) — all environment variables
- [Email setup](email.md) — configure email providers
- [Storage setup](storage.md) — configure object storage
- [Deployment guide](deployment.md) — deploy to production
