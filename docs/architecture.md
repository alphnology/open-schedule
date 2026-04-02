# Architecture

---

## System overview

```
┌──────────────────────────────────────────────────────┐
│                    Browser / PWA                      │
└───────────────────────┬──────────────────────────────┘
                        │ HTTPS
┌───────────────────────▼──────────────────────────────┐
│               Traefik (reverse proxy)                 │
│          TLS termination · HTTP→HTTPS redirect        │
└───────────────────────┬──────────────────────────────┘
                        │ HTTP
┌───────────────────────▼──────────────────────────────┐
│              Open Schedule (Spring Boot)              │
│                                                       │
│  ┌────────────┐  ┌───────────┐  ┌─────────────────┐  │
│  │ Vaadin UI  │  │ REST API  │  │  Security Layer │  │
│  │ (server-   │  │ /share/** │  │  Spring Security│  │
│  │  side)     │  └───────────┘  └─────────────────┘  │
│  └────────────┘                                       │
│  ┌────────────────────────────────────────────────┐   │
│  │              Service Layer                      │   │
│  │  SessionService · SpeakerService · UserService  │   │
│  │  AttenderService · EventService · RatingService │   │
│  │  EmailOpenScheduleService · QrService           │   │
│  └────────────────────────────────────────────────┘   │
│  ┌──────────────────┐  ┌──────────────────────────┐   │
│  │  Storage Layer   │  │     Email Layer          │   │
│  │  ObjectStorage   │  │  EmailService (SMTP)     │   │
│  │  Service (S3)    │  │  PostalEmailService      │   │
│  └────────┬─────────┘  └──────────┬───────────────┘   │
└───────────┼────────────────────────┼──────────────────┘
            │                        │
     ┌──────▼──────┐         ┌───────▼──────┐
     │  SeaweedFS  │         │  SMTP server │
     │  (S3 API)   │         │  or Postal   │
     └─────────────┘         └──────────────┘
            │
     ┌──────▼──────┐
     │  PostgreSQL │
     │  + Flyway   │
     └─────────────┘
```

---

## Layer architecture

### Views (`com.alphnology.views`)

Vaadin server-side views organized by feature:

| Package | Views |
|---------|-------|
| `views.admin` | AdminDashboardView, SessionView, SpeakerView, RoomView, TrackView, TagView, UserView, AttenderView, EventView, NewsView |
| `views.schedule` | ScheduleView, ScheduleViewCard, ScheduleViewDetails |
| `views.speakers` | SpeakersView, SpeakersViewCard, SpeakersViewDetails, VCardView |
| `views.login` | LoginView, SignUpView, ChangePasswordView, ForgotPasswordView, LogoutView |
| `views.rate` | RateView, RatingDialog, StarRating |
| `views.favorite` | FavoriteView |
| `views.news` | NewsView |

All admin views require `ADMIN` role. Public views are accessible to authenticated users unless annotated with `@AnonymousAllowed`.

### Application services (`com.alphnology.services`)

Business logic and orchestration. Each service wraps a Spring Data JPA repository. Services do not expose repositories directly — they expose typed query methods.

### Infrastructure (`com.alphnology.infrastructure`)

External integrations isolated from business logic:

| Package | Responsibility |
|---------|----------------|
| `infrastructure.storage` | S3-compatible object storage (SeaweedFS / MinIO) |
| `services.email` | Email dispatch (SMTP + Postal HTTP API) |
| `services.template` | Thymeleaf template rendering for emails |

### Security (`com.alphnology.security`)

- Spring Security with Vaadin integration
- BCrypt password hashing
- `AuthenticatedUser` — thin wrapper to get the current user
- `UserDetailsServiceImpl` — loads user by username from DB
- Static resources (images, icons, line-awesome) are public
- `/share/**` routes are anonymous (for QR code sharing)
- All other routes require authentication

---

## Data model (key entities)

```
Event ──────────────────────────────────────────────
  │
  ├── Room (name, color)
  ├── Track (name, color)
  └── Tag (name)

Session ─────────────────────────────────────────────
  ├── startTime, endTime
  ├── type (KEYNOTE, TALK, WORKSHOP, LIGHTNING, PANEL)
  ├── level (BEGINNER, INTERMEDIATE, ADVANCED)
  ├── language
  ├── Room (FK)
  ├── Track (FK)
  ├── Speaker[] (M:N)
  └── Tag[] (M:N)

Speaker ─────────────────────────────────────────────
  ├── name, company, title
  ├── bio, country, email
  ├── photoKey (S3 object key)
  └── networking (JSONB)

User ────────────────────────────────────────────────
  ├── username, password (BCrypt)
  ├── name, roles (ADMIN, USER)
  ├── favoriteSessions (JSONB)
  ├── lastLoginTs
  ├── locked (boolean)
  └── oneLogPwd (force change on next login)

SessionRating ────────────────────────────────────────
  ├── User (FK)
  ├── Session (FK)
  └── score

Attender ─────────────────────────────────────────────
  └── (registration/check-in tracking)
```

---

## Email pipeline

```
User action (forgot password, signup, share)
    │
    ▼
EmailOpenScheduleService
    │  Renders Thymeleaf template with model data
    ▼
EmailMessage (to, from, subject, html body, attachments)
    │
    ▼
MailSenderService
    │
    ▼
MailSettingsService ──> mail_settings table + application.email defaults
    │
    ├── provider = POSTAL   → PostalMailTransport → HTTP POST /api/v1/send/message
    └── provider = SMTP/*   → SmtpMailTransport   → Jakarta Mail → SMTP server
```

---

## Storage pipeline

```
File upload (speaker photo)
    │
    ▼
SpeakerView (Vaadin Upload component)
    │  Generates UUID key: speakers/{uuid}.jpg
    ▼
ObjectStorageService.upload(key, inputStream, size, contentType)
    │
    ▼
MinioObjectStorageService → MinIO SDK → SeaweedFS S3 API
    │
    ▼
key stored in speakers.photo_key (PostgreSQL)

Photo retrieval:
    Speaker entity → photoKey → ObjectStorageService.getSignedUrl(key)
                                    → presigned GET URL → browser
```

---

## Technology versions

| Technology | Version | Notes |
|-----------|---------|-------|
| Java | 25 (LTS) | Eclipse Temurin |
| Spring Boot | 4.0.3 | |
| Vaadin | 25.1.1 | Lumo theme |
| Hibernate | 7.2.0 | |
| PostgreSQL | 15 | |
| Flyway | (Spring Boot managed) | |
| MinIO SDK | 8.5.17 | Used as S3 client |
| SeaweedFS | latest | S3-compatible storage |
| Traefik | 3.4 | Reverse proxy |
| Jakarta Mail | (Spring Boot managed) | SMTP client |
