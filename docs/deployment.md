# Deployment — Architecture & Lessons Learned

Gatewell ran in production on Google Cloud Run (`asia-northeast3`, Seoul) with Cloud SQL PostgreSQL. Resources were later torn down to control cost (`deploy/teardown-gcp.sh`). This document keeps the architecture and the issues worth remembering; identifiers (project ID, service accounts, IPs) are intentionally omitted.

## Architecture

```
Browser → Cloud Run (frontend, Next.js) → API proxy → Cloud Run (backend, Spring Boot)
                                                          ↓ Cloud SQL socket factory
                                                      Cloud SQL (PostgreSQL, db-f1-micro)
Images: Artifact Registry · Secrets: Secret Manager (DB password) · Build: Cloud Build
```

- Backend connects to Cloud SQL via the Cloud SQL socket factory (no public IP exposure in app config; credentials injected from Secret Manager at deploy time).
- Frontend never talks to the backend directly from the browser — an API proxy route hides the backend URL.
- Entire setup is reproducible on any GCP project with `deploy/setup-gcp.sh`.

## Issues Encountered

1. **Cloud Build could not upload source** — `403` on the GCS staging bucket. New GCP projects do not always grant the default compute service account Storage access. Fixed by adding the missing IAM role bindings explicitly.
2. **Cloud Run could not read the DB password secret** — `Permission denied on secret`. The revision service account needed `secretmanager.secretAccessor` granted per-secret. Lesson: Cloud Run revisions fail at deploy time, not runtime, when secret bindings are wrong — read the revision error, not the app logs.
3. **Container port mismatch** — Cloud Run injects `PORT`; the app must bind to it rather than a hardcoded port. Fixed in both Dockerfiles.
4. **Instance sizing** — `db-f1-micro` (shared vCPU, 614MB) was sufficient for a free-tier-budget MVP; region chosen for user latency.

## Cost

Estimated ~$8–13/month at MVP traffic (Cloud SQL micro instance dominating). Torn down once the validation goal was met — the deployment scripts make re-deploying a one-command operation.
