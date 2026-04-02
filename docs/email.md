# Email Configuration

Open Schedule sends transactional emails for:
- **Forgot password** — password reset link / temporary password
- **Welcome** — signup confirmation
- **Session share** — share a session with someone

All outbound email is managed by a unified mail subsystem with:
- environment-based defaults in `application.yml`
- optional runtime overrides stored in the database
- an admin UI at `Admin → Mail settings`
- support for **SMTP**, **SendGrid**, **Mailjet**, and **Postal**

The admin UI is only available to authenticated administrators. It can:
- enable or disable outbound email
- choose the provider type
- update sender identity
- update host, port, credentials, timeouts, and security mode
- send a test email
- store secrets encrypted in the database when explicitly enabled

---

## Runtime model

Open Schedule resolves mail settings in this order:

1. Stored admin overrides from the `mail_settings` table
2. Environment defaults from `application.email.*`
3. Provider-specific safe defaults for SendGrid and Mailjet

Secrets are handled differently:
- if `EMAIL_SETTINGS_MASTER_KEY` and `EMAIL_ALLOW_UI_SECRET_PERSISTENCE=true` are configured, admins can store secrets encrypted from the UI
- otherwise, secrets must come from environment variables such as `EMAIL_SMTP_PASSWORD` or `POSTAL_API_KEY`
- the UI never displays the raw secret value

## Provider selection

| Scenario | Configuration |
|----------|---------------|
| Local development | SMTP → Mailpit (default, no credentials needed) |
| Production with SendGrid | SendGrid via SMTP relay |
| Production with Mailjet | Mailjet via SMTP relay |
| Production with Postal | Postal HTTP API (`POSTAL_ENABLED=true`) |
| Disable email entirely | `EMAIL_OUTBOUND_ENABLED=false` or disable in the admin UI |

When `POSTAL_ENABLED=true`, Postal becomes the default provider on first boot unless an admin has already saved a different provider in the UI.

---

## Provider 1 — Generic SMTP

Works with any SMTP server including your own Postfix/Exim, business email, or self-hosted relay.

```bash
EMAIL_OUTBOUND_ENABLED=true
EMAIL_PROVIDER_TYPE=SMTP
EMAIL_SECURITY_MODE=STARTTLS
EMAIL_AUTH_ENABLED=true
EMAIL_SMTP_HOST=mail.yourdomain.com
EMAIL_SMTP_PORT=587
EMAIL_SMTP_USERNAME=no-reply@yourdomain.com
EMAIL_SMTP_PASSWORD=your-smtp-password
EMAIL_FROM_ADDRESS=no-reply@yourdomain.com
EMAIL_FROM_NAME=Open Schedule
```

For servers with self-signed TLS certificates (e.g. internal mail relays):
```bash
EMAIL_SMTP_SSL_TRUST=mail.yourdomain.com
```

Leave `EMAIL_SMTP_SSL_TRUST` blank for all public providers — they have valid certificates.

---

## Provider 2 — SendGrid

SendGrid offers a free tier (100 emails/day) and a reliable SMTP relay.

**Prerequisites:** Verify your sender domain in the SendGrid dashboard under *Settings → Sender Authentication*.

```bash
EMAIL_OUTBOUND_ENABLED=true
EMAIL_PROVIDER_TYPE=SENDGRID
EMAIL_SMTP_HOST=smtp.sendgrid.net
EMAIL_SMTP_PORT=587
EMAIL_SMTP_USERNAME=apikey
EMAIL_SMTP_PASSWORD=SG.xxxxxxxxxxxxxxxxxxxx   # your SendGrid API key
EMAIL_FROM_ADDRESS=no-reply@yourdomain.com
EMAIL_FROM_NAME=Open Schedule
```

> The username is literally the string `apikey` — SendGrid uses the API key as the password.

---

## Provider 3 — Mailjet

Mailjet offers a free tier (200 emails/day) and simple SMTP relay.

**Prerequisites:** Verify your sender domain in Mailjet under *Account → Sender domains & addresses*.

```bash
EMAIL_OUTBOUND_ENABLED=true
EMAIL_PROVIDER_TYPE=MAILJET
EMAIL_SMTP_HOST=in-smtp.mailjet.com
EMAIL_SMTP_PORT=587
EMAIL_SMTP_USERNAME=your-mailjet-api-key
EMAIL_SMTP_PASSWORD=your-mailjet-secret-key
EMAIL_FROM_ADDRESS=no-reply@yourdomain.com
EMAIL_FROM_NAME=Open Schedule
```

Get your API key and Secret key from the [Mailjet API Key Management](https://app.mailjet.com/account/apikeys) page.

---

## Provider 4 — Postal (self-hosted, HTTP API)

[Postal](https://postalserver.io/) is a fully open-source mail delivery platform. Open Schedule integrates with its HTTP API directly — **not via SMTP**.

**Prerequisites:**
1. A running Postal installation
2. An organization, server, and credential created in Postal
3. Your sender domain added and DNS records verified

```bash
POSTAL_ENABLED=true
EMAIL_PROVIDER_TYPE=POSTAL
EMAIL_OUTBOUND_ENABLED=true
POSTAL_BASE_URL=https://postal.yourdomain.com
POSTAL_API_KEY=your-postal-server-api-key
POSTAL_API_KEY_HEADER=X-Server-API-Key

# Sender info (shared with all providers)
EMAIL_FROM_ADDRESS=no-reply@yourdomain.com
EMAIL_FROM_NAME=Open Schedule
```

**How to get the API key:**
1. Log in to your Postal instance
2. Go to your mail server → *Credentials*
3. Create an API credential
4. Copy the key

**API call details (for debugging):**

```
POST https://postal.yourdomain.com/api/v1/send/message
X-Server-API-Key: your-api-key
Content-Type: application/json

{
  "from": "Open Schedule <no-reply@yourdomain.com>",
  "to": ["user@example.com"],
  "subject": "Your subject",
  "html_body": "<h1>Hello</h1>"
}
```

---

## Development — Mailpit

For local development, the default configuration points to Mailpit, a lightweight email catcher that captures all outbound email without delivering it.

```bash
# These are the defaults in .env.dist — no changes needed for dev
EMAIL_OUTBOUND_ENABLED=true
EMAIL_PROVIDER_TYPE=SMTP
EMAIL_SMTP_HOST=localhost
EMAIL_SMTP_PORT=1025
EMAIL_SMTP_USERNAME=dev@example.com
EMAIL_SMTP_PASSWORD=
EMAIL_FROM_ADDRESS=no-reply@example.com
EMAIL_FROM_NAME=Open Schedule
```

Start Mailpit with Docker:
```bash
docker compose up -d mailpit
```

View captured emails at `http://localhost:8025`.

---

## Admin UI workflow

1. Sign in as an administrator
2. Open `Admin → Mail settings`
3. Select the provider type
4. Fill in sender identity and provider fields
5. Save the configuration
6. Enter a `Test recipient` address
7. Click `Send test email`

If encrypted secret persistence is disabled, the view will tell you that secrets must be provided through environment variables.

## Testing email in production

Recommended flow:

1. Configure the provider from environment variables and/or the admin UI
2. Save settings from `Admin → Mail settings`
3. Send a test email from the admin view
4. Validate provider-side logs or delivery dashboards

Fallback flow:

1. Open the app and click `Forgot password`
2. Enter a valid user's email address
3. Check your provider logs for delivery status

For Postal, the Postal web UI shows message delivery status, bounces, and logs per message.

---

## Security considerations

- Store `EMAIL_SMTP_PASSWORD`, `POSTAL_API_KEY`, and `EMAIL_SETTINGS_MASTER_KEY` in a secrets manager, not in plain `.env` files on the server
- Verify your sending domain with SPF, DKIM, and DMARC records to avoid spam filtering
- Do not set `EMAIL_SMTP_SSL_TRUST` in production unless you genuinely need to bypass certificate validation
- Only enable `EMAIL_ALLOW_UI_SECRET_PERSISTENCE=true` when you also provide a strong `EMAIL_SETTINGS_MASTER_KEY`
- Rotate API keys periodically

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Emails not sent, no error in logs | Outbound email disabled | Set `EMAIL_OUTBOUND_ENABLED=true` or enable outbound email in the admin UI |
| `Authentication failed` | Wrong credentials | Verify `EMAIL_SMTP_USERNAME` / `PASSWORD` |
| `Connection refused` | Wrong host/port | Check `EMAIL_SMTP_HOST` and `EMAIL_SMTP_PORT` |
| Emails go to spam | Domain not verified | Add SPF/DKIM/DMARC records |
| SendGrid 403 | Sender not verified | Verify sender in SendGrid dashboard |
| Postal `non-success status` | Invalid API key or server URL | Check `POSTAL_BASE_URL` (no trailing slash) and `POSTAL_API_KEY` |
| Self-signed TLS error | Cert not trusted | Set `EMAIL_SMTP_SSL_TRUST=your-smtp-host` |
| UI cannot store the secret | Secret persistence disabled | Configure `EMAIL_SETTINGS_MASTER_KEY` and `EMAIL_ALLOW_UI_SECRET_PERSISTENCE=true`, or keep the secret in environment variables |
