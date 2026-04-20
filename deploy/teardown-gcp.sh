#!/bin/bash
set -euo pipefail

# ============================================================
# Gatewell GCP Teardown - removes all resources to stop billing
# ============================================================

REGION="${GCP_REGION:-asia-northeast3}"

echo "=== Tearing down Gatewell GCP resources ==="
echo "WARNING: This will delete all data!"
read -p "Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi

echo "Deleting Cloud Run services..."
gcloud run services delete gatewell-api --region "$REGION" --quiet 2>/dev/null || true
gcloud run services delete gatewell-frontend --region "$REGION" --quiet 2>/dev/null || true

echo "Deleting Cloud SQL instance..."
gcloud sql instances delete gatewell-db --quiet 2>/dev/null || true

echo "Deleting secrets..."
gcloud secrets delete gatewell-db-password --quiet 2>/dev/null || true

echo "Deleting container images..."
gcloud artifacts repositories delete gatewell --location="$REGION" --quiet 2>/dev/null || true

echo ""
echo "=== Teardown complete ==="
