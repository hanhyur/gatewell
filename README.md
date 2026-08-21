# Gatewell

Free website security scanner for side projects, startups, and vibe-coded apps.

Paste your URL → get an instant security report with step-by-step fix guides.

**Status:** Not currently live. Previously deployed to Google Cloud Run at `gatewell.dev`; resources were torn down to control cost. See [docs/deployment.md](docs/deployment.md) for the deployment architecture and lessons learned. Runs locally with one command (below).

---

## What It Does

Gatewell scans your live website or GitHub repository for common security vulnerabilities and gives you a clear verdict: **BLOCK**, **CAUTION**, or **ALLOW**.

Each finding includes:
- A plain-language explanation of the risk
- The real-world impact
- Copy-paste fix guides for Vercel, Netlify, Nginx, and more

No signup required. Free to use.

## What It Checks

### URL Scan
| Category | Checks |
|----------|--------|
| Security Headers | HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy |
| CORS | Wildcard origins, origin reflection, credentials misconfiguration |
| Cookies | HttpOnly, Secure, SameSite flags |
| SSL/TLS | HTTPS enforcement, HTTP accessibility, redirect behavior |
| Exposed Paths | /.env, /.git, /actuator, /admin, /debug, /phpinfo |
| Error Handling | Stack trace exposure, reflected XSS detection |
| Information Leakage | Server header, X-Powered-By |
| Bot Protection | Cloudflare, AWS WAF, CAPTCHA detection |

### GitHub Scan
| Category | Checks |
|----------|--------|
| Hardcoded Secrets | AWS keys, GitHub tokens, Stripe keys, Google API keys, JWTs, DB URLs, private keys (11 patterns) |
| Sensitive Files | .env, credentials.json, .pem, id_rsa |
| Code Vulnerabilities | SQL injection, eval(), innerHTML/dangerouslySetInnerHTML, CORS wildcard, disabled SSL verification |

> GitHub scan supports public repositories only.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend | Kotlin 2.3 + Spring Boot 4.0 |
| Frontend | Next.js 16 + TypeScript + Tailwind CSS 4 |
| Database | PostgreSQL (Cloud SQL) |
| Infrastructure | Google Cloud Run (deployed, later decommissioned) |

## Project Structure

```
gatewell/
├── src/main/kotlin/me/hanhyur/gatewell/
│   ├── scanner/              # URL & GitHub security scanners
│   │   ├── UrlSecurityScanner.kt
│   │   ├── GitHubCodeScanner.kt
│   │   ├── FindingGuide.kt   # Human-readable fix guides
│   │   ├── ScanController.kt
│   │   └── persistence/      # Scan results, usage tracking, email leads
│   ├── auth/                 # API key authentication
│   ├── assessment/           # Risk assessment engine (14 rules, 9 categories)
│   ├── common/               # CORS, security headers, error handling
│   └── harness/              # Regression test harness
├── frontend/
│   ├── app/
│   │   ├── scan/             # Scan input page
│   │   ├── report/[id]/      # Shareable scan result page
│   │   └── api/scan/         # API proxy routes (hides backend URL)
│   ├── components/
│   │   ├── Nav.tsx
│   │   └── ScanResults.tsx   # Finding cards with fix guides
│   └── lib/
│       ├── scan-api.ts
│       └── server-config.ts
├── deploy/
│   ├── setup-gcp.sh          # One-command GCP deployment
│   └── teardown-gcp.sh       # Resource cleanup
├── docs/
│   └── deployment.md         # Deployment architecture & lessons learned
├── prompts/                  # AI workflow prompts (research → plan → tasks → implement → review)
├── Dockerfile                # Backend container
├── docker-compose.yml        # Local dev (backend + frontend + PostgreSQL)
└── build.gradle
```

## Getting Started

### Prerequisites

- JDK 21
- Node.js 22+
- Docker (for local PostgreSQL)

### Local Development

```bash
# Backend
./gradlew bootRun

# Frontend (in a separate terminal)
cd frontend
npm install
npm run dev
```

Backend runs on `http://localhost:8080`, frontend on `http://localhost:3000`.

### Docker

```bash
DB_PASSWORD=your_password docker compose up -d
```

- Frontend: `http://localhost:3001`
- Backend: `http://localhost:8081`

### Run Tests

```bash
./gradlew test
```

## Deployment (GCP)

Previously deployed on Google Cloud Run with Cloud SQL PostgreSQL. The deployment scripts remain reproducible on any GCP project.

```bash
# First time setup
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# Deploy
./deploy/setup-gcp.sh

# Cleanup
./deploy/teardown-gcp.sh
```

See [docs/deployment.md](docs/deployment.md) for the architecture and issues encountered during deployment.

## Security

Gatewell scans itself and gets **ALLOW** with 0 actionable findings.

Applied security measures:
- All security response headers (HSTS, CSP, X-Frame-Options, etc.)
- API key authentication with SHA-256 hashing
- SSRF protection (private IP, loopback, metadata endpoint blocking)
- Rate limiting (3 scans/day per IP)
- API proxy (backend URL not exposed to browser)
- Cloud SQL SSL enforced, automated backups
- Swagger/API docs disabled in production

## How This Was Built — AI Workflow

This project was built with AI coding agents (Claude Code, Codex CLI) operating under explicit rules, with every stage gated by human review.

- **Pipeline:** research → plan → tasks → implement → review. Each stage has a fixed prompt in [`prompts/`](prompts/), and each stage writes its output to `docs/` so the next stage works from a reviewed document, not chat history.
- **Rules:** [`AGENTS.md`](AGENTS.md) defines domain constraints and quality bars; [`CODEX.md`](CODEX.md) defines how agents must behave in this codebase.
- **Gates:** agent output only lands after passing tests (`./gradlew test`), the review-stage checklist, and a self-scan (Gatewell scanning its own deployment) — see the PR history for the audit-fix cycles this produced.

The prompts are preserved as-is, including their informal tone — they are the actual working artifacts, not documentation written after the fact.

## License

All rights reserved. Source is public for portfolio and reference purposes.
