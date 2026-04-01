# Storage Configuration

Open Schedule uses any S3-compatible object storage backend to store speaker photos and file attachments. The storage layer is fully abstracted — switching providers requires only environment variable changes.

**Default for local dev:** SeaweedFS (included in `docker-compose.yml`)
**Recommended for production:** SeaweedFS, MinIO, AWS S3, or Cloudflare R2

---

## How it works

The app uses the MinIO Java SDK as its S3 client. Because SeaweedFS, MinIO, AWS S3, and most S3-compatible services implement the same API, the SDK works identically against any of them — only the endpoint and credentials differ.

Objects are stored with a generated key (`speakers/{uuid}.jpg`). Signed URLs with configurable expiry are used for public access.

---

## Provider 1 — SeaweedFS (recommended for self-hosting)

SeaweedFS is a lightweight, high-performance distributed object store with a built-in S3-compatible API. It is the default in the development `docker-compose.yml`.

**Local dev (Docker):**

```bash
docker compose up -d seaweedfs
# S3 API available at http://localhost:8333
```

```bash
STORAGE_ENDPOINT=http://localhost:8333
STORAGE_ACCESS_KEY=any
STORAGE_SECRET_KEY=any
STORAGE_BUCKET=open-schedule
STORAGE_PUBLIC_ENDPOINT=
STORAGE_SIGNED_URL_EXPIRY=3600
```

SeaweedFS runs without authentication by default. For production, enable IAM authentication in the SeaweedFS configuration and set real credentials.

**Production Docker Compose snippet:**

```yaml
seaweedfs:
  image: chrislusf/seaweedfs:latest
  command: server -s3 -s3.port=8333 -dir=/data -master.volumeSizeLimitMB=30000 -ip.bind=0.0.0.0
  ports:
    - "8333:8333"
  volumes:
    - seaweedfs_data:/data
  restart: unless-stopped
```

---

## Provider 2 — MinIO

```bash
STORAGE_ENDPOINT=http://minio.yourdomain.com:9000
STORAGE_ACCESS_KEY=your-minio-access-key
STORAGE_SECRET_KEY=your-minio-secret-key
STORAGE_BUCKET=open-schedule
STORAGE_PUBLIC_ENDPOINT=https://minio-public.yourdomain.com
STORAGE_SIGNED_URL_EXPIRY=3600
```

Create the bucket before starting the app, or let the app create it automatically on first upload (`ensureBucketExists()`).

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
STORAGE_ENDPOINT=http://new-provider:8333
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
rclone copy seaweedfs:open-schedule ./backup-$(date +%Y%m%d)/

# Or using AWS CLI pointed at your S3-compatible endpoint
AWS_ACCESS_KEY_ID=any AWS_SECRET_ACCESS_KEY=any \
  aws s3 sync s3://open-schedule ./backup-$(date +%Y%m%d) \
  --endpoint-url http://localhost:8333
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Speaker photo not showing | Wrong `STORAGE_PUBLIC_ENDPOINT` | Set it to the publicly accessible URL |
| Upload fails silently | Bucket doesn't exist and auto-create failed | Check `STORAGE_ACCESS_KEY` permissions and logs |
| Signed URL expired | `STORAGE_SIGNED_URL_EXPIRY` too low | Increase value (e.g. `86400` for 24 hours) |
| `Connection refused` to storage | SeaweedFS not running | `docker compose up -d seaweedfs` |
| `Invalid credentials` | Wrong access/secret key | Verify `STORAGE_ACCESS_KEY` and `STORAGE_SECRET_KEY` |
