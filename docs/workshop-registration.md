# Workshop Registration

Open Schedule includes a public workshop-registration flow backed by alf.io ticket validation.

## What it does

- Exposes a public route at `/workshop-registration`
- Lets attendees enter the alf.io ticket reference shown as `Número de referencia`
- Validates that reference against the configured alf.io admin reservation endpoint
- Shows only sessions marked as `SessionType.W`
- Enforces workshop capacity from `session.room.capacity`
- Treats `room.capacity = 0` as unlimited
- Prevents duplicate registrations for the same ticket in the same event

## Administrative setup

Route:

- `Admin -> Workshop registration`

The admin view stores:

- Module enabled / disabled
- Public flow active / inactive
- Alf.io base URL
- Event slug
- Alf.io bearer token
- Optional public message
- Workshop-per-ticket limit

## Security model

- The public route does not require login
- The admin route requires the `ADMIN` role
- The alf.io token never reaches the browser
- The token is encrypted before being stored in the database

### Required secret-persistence settings

The alf.io token is persisted with the same encrypted-secret mechanism used by the mail settings module.

You must configure:

```bash
APP_SECRETS_MASTER_KEY=replace-with-a-strong-random-secret
APP_SECRETS_ALLOW_UI_PERSISTENCE=true
```

Without those settings, the admin UI cannot safely persist the alf.io token.

## alf.io contract

Configured request pattern:

```http
GET {alfioBaseUrl}/api/v1/admin/event/{eventSlug}/reservation/{reservationId}
Authorization: Bearer {token}
```

Example:

```http
GET https://tickets.jconfdominicana.org/api/v1/admin/event/jd2026/reservation/b2b2b49e-881e-40f9-895f-52a995e84bfd
Authorization: Bearer <token>
```

The participant must enter:

- The UUID-like value shown in the PDF as `Número de referencia`

Not the short reservation code.

## Registration rules

- Only workshop sessions (`SessionType.W`) are listed
- A workshop must have a room assigned
- If `room.capacity > 0`, seats are limited to that number
- If `room.capacity = 0`, seats are unlimited
- One active registration per ticket is enforced

## Admin management

The admin screen supports:

- Searching by attendee name
- Searching by attendee email
- Searching by ticket reference
- Searching by reservation short code
- Filtering registrations by workshop
- Moving a registration to another workshop
- Deleting a registration so the ticket can register again

## Database objects

Flyway migration:

- `src/main/resources/db/migration/prod/V0.0.5__workshop_registration.sql`

Tables:

- `workshop_registration_settings`
- `workshop_participant_registration`

Uniqueness:

- `UNIQUE(event_slug, ticket_reference)`

## Troubleshooting

### “Workshop registration is currently unavailable”

Check:

- The module is enabled
- The public flow is active
- Base URL is set
- Event slug is set
- A token is stored

### “No workshop seats are currently available”

Check:

- At least one session is marked as `SessionType.W`
- The session has a room assigned
- The room capacity is not already exhausted

### “We could not validate this ticket”

Check:

- The ticket reference is the full `Número de referencia`
- The alf.io event slug is correct
- The bearer token is valid
- The alf.io reservation is in a valid state such as `COMPLETE`
