# Email Configuration

Open Schedule sends transactional emails for:
- **Forgot password** — password reset link / temporary password
- **Welcome** — signup confirmation
- **Session share** — share a session with someone

All emails are triggered by authenticated user actions or system flows. There is no anonymous outbound email.

Two provider implementations are available:
1. **SMTP** — works with any SMTP server (default)
2. **Postal HTTP API** — for self-hosted [Postal](https://postalserver.io/) installations

---

## Provider selection

| Scenario | Configuration |
|----------|---------------|
| Local development | SMTP → Mailpit (default, no credentials needed) |
| Production with SendGrid | SMTP → SendGrid SMTP relay |
| Production with Mailjet | SMTP → Mailjet SMTP relay |
| Production with Postal | Postal HTTP API (`POSTAL_ENABLED=true`) |
| Disable email entirely | `EMAIL_SMTP_ENABLED=false` |

When `POSTAL_ENABLED=true`, Postal takes over and SMTP is ignored.

---

## Provider 1 — Generic SMTP

Works with any SMTP server including your own Postfix/Exim, business email, or self-hosted relay.

```bash
EMAIL_SMTP_ENABLED=true
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
EMAIL_SMTP_ENABLED=true
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
EMAIL_SMTP_ENABLED=true
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
POSTAL_BASE_URL=https://postal.yourdomain.com
POSTAL_API_KEY=your-postal-server-api-key

# Disable SMTP to avoid loading both providers
EMAIL_SMTP_ENABLED=false

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
EMAIL_SMTP_ENABLED=true
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

## Testing email in production

Trigger a test email using the "Forgot Password" flow:

1. Open the app and click "Forgot password"
2. Enter a valid user's email address
3. Check your SMTP provider's dashboard or logs for delivery status

For Postal: the Postal web UI shows message delivery status, bounces, and logs per message.

---

## Security considerations

- Store `EMAIL_SMTP_PASSWORD` and `POSTAL_API_KEY` in a secrets manager, not in plain `.env` files on the server
- Verify your sending domain with SPF, DKIM, and DMARC records to avoid spam filtering
- Do not set `EMAIL_SMTP_SSL_TRUST` in production unless you genuinely need to bypass certificate validation
- Rotate API keys periodically

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Emails not sent, no error in logs | `EMAIL_SMTP_ENABLED=false` | Set to `true` |
| `Authentication failed` | Wrong credentials | Verify `EMAIL_SMTP_USERNAME` / `PASSWORD` |
| `Connection refused` | Wrong host/port | Check `EMAIL_SMTP_HOST` and `EMAIL_SMTP_PORT` |
| Emails go to spam | Domain not verified | Add SPF/DKIM/DMARC records |
| SendGrid 403 | Sender not verified | Verify sender in SendGrid dashboard |
| Postal `non-success status` | Invalid API key or server URL | Check `POSTAL_BASE_URL` (no trailing slash) and `POSTAL_API_KEY` |
| Self-signed TLS error | Cert not trusted | Set `EMAIL_SMTP_SSL_TRUST=your-smtp-host` |
