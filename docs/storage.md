# Storage Configuration

Open Schedule uses any S3-compatible object storage backend to store speaker photos and file attachments. The storage layer is fully abstracted — switching providers requires only environment variable changes.

**Default for local dev:** MinIO (included in `docker-compose.yml`)
**Recommended for production:** MinIO, AWS S3, SeaweedFS, or Cloudflare R2

---

## How it works

The app uses the MinIO Java SDK as its S3-compatible client. Because SeaweedFS, MinIO, AWS S3, and most S3-compatible services implement the same API, the SDK works identically against any of them — only the endpoint and credentials differ.

Objects are stored with a generated key (`speakers/{uuid}.jpg`). Signed URLs with configurable expiry are used for public access.

---

## Provider 1 — MinIO (default for local development)

MinIO is the default in the development `docker-compose.yml`. It is simple, explicit, and predictable for local testing.

**Local dev (Docker):**

```bash
docker compose up -d minio
# S3 API available at http://localhost:9000
# Console available at http://localhost:9001
```

```bash
STORAGE_ENDPOINT=http://localhost:9000
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin
STORAGE_BUCKET=open-schedule
STORAGE_PUBLIC_ENDPOINT=
STORAGE_SIGNED_URL_EXPIRY=3600
```

For production, replace the default development credentials with real ones and restrict network access.

**Production Docker Compose snippet:**

```yaml
minio:
  image: minio/minio:latest
  command: server /data --console-address ":9001"
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    - MINIO_ROOT_USER=minioadmin
    - MINIO_ROOT_PASSWORD=minioadmin
  volumes:
    - minio_data:/data
  restart: unless-stopped
```

---

## Provider 2 — SeaweedFS

```bash
STORAGE_ENDPOINT=http://localhost:8333
STORAGE_ACCESS_KEY=your-seaweedfs-access-key
STORAGE_SECRET_KEY=your-seaweedfs-secret-key
STORAGE_BUCKET=open-schedule
STORAGE_PUBLIC_ENDPOINT=
STORAGE_SIGNED_URL_EXPIRY=3600
```

SeaweedFS can also work, but bucket/IAM behavior depends on how you configure auth on the server.

---

## Provider 3 — AWS S3

```bash
STORAGE_ENDPOINT=https://s3.amazonaws.com
STORAGE_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE
STORAGE_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
STORAGE_BUCKET=open-schedule-prod
STORAGE_PUBLIC_ENDPOINT=https://s3.amazonaws.com
STORAGE_SIGNED_URL_EXPIRY=3600
```

Create the S3 bucket manually in the AWS console and set appropriate bucket policy. The IAM user needs `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, and `s3:HeadObject` on the bucket.

---

## Provider 4 — Cloudflare R2

Cloudflare R2 is S3-compatible and has no egress fees.

```bash
STORAGE_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
STORAGE_ACCESS_KEY=your-r2-access-key-id
STORAGE_SECRET_KEY=your-r2-secret-access-key
STORAGE_BUCKET=open-schedule
STORAGE_PUBLIC_ENDPOINT=https://pub-<hash>.r2.dev   # public bucket URL if enabled
STORAGE_SIGNED_URL_EXPIRY=3600
```

---

## Public endpoint vs. internal endpoint

| Variable | Purpose |
|----------|---------|
| `STORAGE_ENDPOINT` | Used by the application server to upload and manage objects (internal network) |
| `STORAGE_PUBLIC_ENDPOINT` | Used to generate signed URLs for the browser to fetch objects (public internet) |

In a typical deployment, `STORAGE_ENDPOINT` points to an internal service address and `STORAGE_PUBLIC_ENDPOINT` points to the public-facing URL. If left blank, `STORAGE_PUBLIC_ENDPOINT` defaults to `STORAGE_ENDPOINT`.

---

## Migrating between providers

Because the storage interface is fully abstracted, migration is a copy + env-var swap:

```bash
# 1. Copy objects from old provider to new provider using rclone
rclone copy old-provider:open-schedule new-provider:open-schedule --progress

# 2. Verify object count
rclone ls old-provider:open-schedule | wc -l
rclone ls new-provider:open-schedule | wc -l

# 3. Update .env to point to new provider
STORAGE_ENDPOINT=http://new-provider:9000
STORAGE_ACCESS_KEY=new-key
STORAGE_SECRET_KEY=new-secret

# 4. Restart the application
docker compose restart app-open-schedule
```

No database changes are needed. Object keys are stored as-is in the `speakers.photo_key` column.

---

## Backup

```bash
# Backup all objects to local directory using rclone
rclone copy minio:open-schedule ./backup-$(date +%Y%m%d)/

# Or using AWS CLI pointed at your S3-compatible endpoint
AWS_ACCESS_KEY_ID=minioadmin AWS_SECRET_ACCESS_KEY=minioadmin \
  aws s3 sync s3://open-schedule ./backup-$(date +%Y%m%d) \
  --endpoint-url http://localhost:9000
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Speaker photo not showing | Wrong `STORAGE_PUBLIC_ENDPOINT` | Set it to the publicly accessible URL |
| Upload fails silently | Bucket doesn't exist and auto-create failed | Check `STORAGE_ACCESS_KEY` permissions and logs |
| Signed URL expired | `STORAGE_SIGNED_URL_EXPIRY` too low | Increase value (e.g. `86400` for 24 hours) |
| `Connection refused` to storage | MinIO not running | `docker compose up -d minio` |
| `Invalid credentials` | Wrong access/secret key | Verify `STORAGE_ACCESS_KEY` and `STORAGE_SECRET_KEY` |
