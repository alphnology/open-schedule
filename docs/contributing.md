# Contributing to Open Schedule

Thank you for your interest in contributing. This guide covers everything you need to get started.

---

## Ways to contribute

- Report bugs via [GitHub Issues](https://github.com/alphnology/open-schedule/issues)
- Request features via [GitHub Issues](https://github.com/alphnology/open-schedule/issues)
- Fix bugs or implement features via Pull Requests
- Improve documentation
- Translate the UI (i18n)
- Share the project

---

## Development setup

Follow [Getting Started](getting-started.md) to set up the local development environment.

Quick summary:
```bash
git clone https://github.com/alphnology/open-schedule.git
cd open-schedule
cp .env.dist .env
docker compose up -d
./mvnw spring-boot:run -Pdev
```

---

## Git workflow

1. Fork the repository on GitHub
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/open-schedule.git
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/alphnology/open-schedule.git
   ```
4. Create a branch from `dev` (not `main`):
   ```bash
   git checkout dev
   git pull upstream dev
   git checkout -b feat/your-feature-name
   ```
5. Make your changes
6. Push to your fork and open a PR against the `dev` branch

---

## Commit conventions

We use [Conventional Commits](https://www.conventionalcommits.org/). The format is:

```
<type>(<scope>): <short description>
```

**Types:**

| Type | When to use |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `style` | Formatting, whitespace — no logic change |
| `test` | Adding or modifying tests |
| `chore` | Build scripts, dependencies, CI |

**Examples:**
```
feat(sessions): add CSV export for session list
fix(email): correct @ConditionalOnProperty prefix for SMTP service
docs(storage): add Cloudflare R2 configuration example
refactor(speaker-view): extract empty state into reusable component
```

---

## Code style

- **Java:** Follow the existing code style. The project includes `eclipse-formatter.xml` — import it in your IDE.
- **No Lombok abuse:** Use `@Getter`, `@Setter`, `@RequiredArgsConstructor` as the existing code does. Avoid `@Data` on JPA entities.
- **No unnecessary comments:** Only comment logic that isn't self-evident.
- **LumoUtility over inline styles:** Use Vaadin's `LumoUtility` classes for spacing, color, and typography in Views.
- **Service layer, not repositories in Views:** Views call Services. Services call Repositories. Never inject a Repository into a View.

---

## Pull request checklist

Before opening a PR, verify:

```
[ ] Branch is based on dev, not main
[ ] Code compiles: ./mvnw compile
[ ] Tests pass: ./mvnw test
[ ] New feature has at least one test (if testable without a browser)
[ ] Commit messages follow Conventional Commits
[ ] No secrets or credentials added to any file
[ ] .env is not committed
[ ] Documentation updated if behavior changed
```

---

## What makes a good PR

- **Small and focused** — one concern per PR. A bug fix and a refactor are two separate PRs.
- **Explained** — fill in the PR template. Describe what changed and why.
- **Tested** — if you touched business logic, add or update a test.
- **Reviewed your own diff first** — read through the changes before requesting review.

---

## Reporting bugs

Use the [bug report template](https://github.com/alphnology/open-schedule/issues/new?template=bug_report.yml).

Include:
- What you expected to happen
- What actually happened
- Steps to reproduce
- Open Schedule version (`application.version` in the footer)
- Java and OS version

---

## Requesting features

Use the [feature request template](https://github.com/alphnology/open-schedule/issues/new?template=feature_request.yml).

Explain the problem you're trying to solve, not just the solution. Good feature requests describe a user need first.

---

## Questions

Open a [GitHub Discussion](https://github.com/alphnology/open-schedule/discussions) for questions that aren't bugs or feature requests.
