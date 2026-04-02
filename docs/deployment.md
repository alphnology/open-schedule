# Deployment Guide

This guide covers deploying Open Schedule to a production server using Docker Compose and Traefik as a reverse proxy with automatic SSL.

---

## Prerequisites

- A Linux server (Ubuntu 22.04+ or Debian 12+ recommended)
- Docker Engine 24+ with Compose v2
- A domain name pointing to your server
- Ports 80 and 443 open in your firewall

---

## Architecture overview

```
Internet
    │
    ▼
Traefik (ports 80/443)
    │  ← HTTP → HTTPS redirect
    │  ← Let's Encrypt TLS
    ▼
Open Schedule app (port 51675, internal)
    │
    ├── PostgreSQL (internal)
    └── MinIO or S3-compatible storage (internal)
```

---

## 1. Prepare the server

```bash
# Add Docker's official GPG key:
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update

# Install Docker packages:
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Verify the installation:
sudo docker run hello-world

# Create a directory for the project
mkdir -p /opt/open-schedule && cd /opt/open-schedule

# Clone the repository
git clone https://github.com/alphnology/open-schedule.git .
```

---

## 2. Configure environment

```bash
cp .env.dist .env
```

Minimum required variables for production:

```bash
# Database
DB_PASSWORD=a-strong-unique-password

# Storage (MinIO running on same host)
STORAGE_ENDPOINT=http://minio:9000
STORAGE_PUBLIC_ENDPOINT=https://storage.yourconference.com   # if publicly accessible

# Email
EMAIL_SMTP_HOST=smtp.sendgrid.net
EMAIL_SMTP_PORT=587
EMAIL_SMTP_USERNAME=apikey
EMAIL_SMTP_PASSWORD=SG.your-sendgrid-key
EMAIL_FROM_ADDRESS=no-reply@yourconference.com
EMAIL_FROM_NAME=Open Schedule

# Application URLs
APP_URL=https://schedule.yourconference.com
EVENT_WEBSITE=https://yourconference.com

# GitHub (optional — for bug reporting)
GIT_HUB_API_TOKEN=
```

---

## 3. Configure Traefik

The `docker-compose-prod.yml` includes Traefik. Before starting, edit the file and replace the placeholders:

```yaml
# In docker-compose-prod.yml, find and replace:
- "traefik.http.routers.app.rule=Host(`openschedule.alphnology.com`)"
# Change to your actual domain:
- "traefik.http.routers.app.rule=Host(`schedule.yourconference.com`)"
```

Also update the Let's Encrypt email:
```yaml
- "--certificatesresolvers.myresolver.acme.email=your-email@yourconference.com"
```

Create the required ACME storage file:
```bash
mkdir -p letsencrypt
touch letsencrypt/acme.json
chmod 600 letsencrypt/acme.json
```

---

## 4. Build the production image

```bash
# Build the JAR with production profile (Vaadin frontend bundled)
./mvnw clean package -DskipTests -Pproduction

# Build the Docker image
docker build -t alphnology/open-schedule:latest .
```

Or pull the latest published image (if available on Docker Hub):
```bash
docker pull alphnology/open-schedule:latest
```

---

## 5. Start the stack

```bash
docker compose -f docker-compose-prod.yml up -d
```

Check that all services are healthy:
```bash
docker compose -f docker-compose-prod.yml ps
docker compose -f docker-compose-prod.yml logs -f app-open-schedule
```

The application performs Flyway migrations on startup. First boot may take 20–30 seconds.

---

## 6. Verify

- `https://schedule.yourconference.com` — should load the login page
- `https://schedule.yourconference.com/actuator/health` — should return `{"status":"UP"}`

---

## Updating to a new version

```bash
# Pull latest code
git pull origin main

# Rebuild
./mvnw clean package -DskipTests -Pproduction
docker build -t alphnology/open-schedule:latest .

# Rolling restart (zero downtime if using Traefik)
docker compose -f docker-compose-prod.yml up -d --no-deps app-open-schedule
```

Flyway applies any new migrations automatically on startup.

---

## Backup and restore

### Database

```bash
# Backup
docker exec postgres pg_dump -U postgres open-schedule | gzip > backup-$(date +%Y%m%d).sql.gz

# Restore
gunzip -c backup-20250510.sql.gz | docker exec -i postgres psql -U postgres open-schedule
```

### Storage

```bash
# Backup all objects
docker run --rm \
  -v /opt/open-schedule/minio-backup:/backup \
  --network open-schedule_default \
  amazon/aws-cli s3 sync s3://open-schedule /backup \
  --endpoint-url http://minio:9000 \
  --no-sign-request
```

---

## Security hardening checklist

```
[ ] DB_PASSWORD is unique and not the default
[ ] STORAGE credentials are not the dev defaults
[ ] Let's Encrypt email is valid
[ ] Traefik dashboard basic auth is set
[ ] Server firewall: only 80/443 public, 51675/5432/9000/9001 internal only
[ ] Regular database backups scheduled (cron)
[ ] APP_URL matches actual public domain
[ ] .env file permissions: chmod 600 .env
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| SSL certificate not issued | Check DNS propagation: `dig schedule.yourconference.com`. Wait up to 5 min after DNS change |
| 502 Bad Gateway | App not started. Check `docker compose logs app-open-schedule` |
| Database migration fails | Check `DB_*` env vars. Run `docker compose logs app-open-schedule \| grep Flyway` |
| Storage uploads fail | Check `STORAGE_*` env vars and MinIO container status |
