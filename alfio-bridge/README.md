# alfio-bridge

Internal bridge service for resolving alf.io reservation data from identifiers that Open Schedule users actually have in hand, starting with `ticket UUID`.

## Scope

- Read-only PostgreSQL access to the alf.io database
- Internal HTTP API secured with an API key
- Resolution of `ticket UUID -> reservationId`
- Optional resolution of `order code -> reservationId`

## Local build

```bash
./mvnw -f alfio-bridge/pom.xml clean package
```

## Docker image

Build the jar first, then build the image:

```bash
./mvnw -f alfio-bridge/pom.xml clean package
docker build -t alphnology/alfio-bridge:latest ./alfio-bridge
```

## Runtime environment

- `ALFIO_DB_HOST`
- `ALFIO_DB_PORT`
- `ALFIO_DB_NAME`
- `ALFIO_DB_USER`
- `ALFIO_DB_PASSWORD`
- `ALFIO_BRIDGE_API_KEY`
- `ALFIO_BRIDGE_PORT`

Copy [`.env.dist`](/Users/fredpena/Development/Alphnology/open-schedule/alfio-bridge/.env.dist) to `.env` or inject the values directly in Docker.

## Primary endpoint

`POST /api/v1/reservations/resolve`

Example request:

```json
{
  "eventSlug": "jd2026",
  "referenceType": "ticket_uuid",
  "referenceValue": "b2b2b49e-881e-40f9-895f-52a995e84bfd"
}
```

## Notes

- The bridge is intentionally internal-only. Do not expose it through Traefik unless you need a controlled admin/debug route.
- The current implementation supports `ticket_uuid`, `order_code`, and `reservation_id`.
- Database access is read-only from the bridge side.
