# Gatewell

Free website security scanner for side projects, startups, and vibe-coded apps.

Paste your URL → get an instant security report with step-by-step fix guides.

**Live:** https://gatewell.dev

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
| Infrastructure | Google Cloud Run (Tokyo) |
| Domain | gatewell.dev |

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
│   └── gcp-deployment-log.md # Deployment history with issues & fixes
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

Deployed on Google Cloud Run (Tokyo region) with Cloud SQL PostgreSQL.

```bash
# First time setup
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# Deploy
./deploy/setup-gcp.sh

# Cleanup
./deploy/teardown-gcp.sh
```

See [docs/gcp-deployment-log.md](docs/gcp-deployment-log.md) for detailed deployment history.

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

## License

Private project.
