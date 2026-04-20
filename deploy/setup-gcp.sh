#!/bin/bash
set -euo pipefail

# ============================================================
# Gatewell GCP Deployment Setup
# Prerequisites: gcloud CLI installed and authenticated
#   brew install google-cloud-sdk
#   gcloud auth login
#   gcloud config set project YOUR_PROJECT_ID
# ============================================================

PROJECT_ID=$(gcloud config get-value project)
REGION="${GCP_REGION:-asia-northeast3}"
DB_INSTANCE="gatewell-db"
DB_NAME="gatewell"
DB_USER="gatewell"
DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -base64 24)}"

echo "=== Gatewell GCP Deployment ==="
echo "Project: $PROJECT_ID"
echo "Region:  $REGION"
echo ""

# --- Step 1: Enable required APIs ---
echo "[1/6] Enabling GCP APIs..."
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  --quiet

# --- Step 2: Create Artifact Registry ---
echo "[2/6] Creating Artifact Registry..."
gcloud artifacts repositories create gatewell \
  --repository-format=docker \
  --location="$REGION" \
  --quiet 2>/dev/null || echo "  (already exists)"

# --- Step 3: Create Cloud SQL instance ---
echo "[3/6] Creating Cloud SQL PostgreSQL instance..."
echo "  (This may take 5-10 minutes on first run)"
gcloud sql instances describe "$DB_INSTANCE" --quiet 2>/dev/null || \
gcloud sql instances create "$DB_INSTANCE" \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region="$REGION" \
  --storage-type=HDD \
  --storage-size=10GB \
  --quiet

# Create database and user
gcloud sql databases create "$DB_NAME" \
  --instance="$DB_INSTANCE" --quiet 2>/dev/null || echo "  (database exists)"

gcloud sql users create "$DB_USER" \
  --instance="$DB_INSTANCE" \
  --password="$DB_PASSWORD" --quiet 2>/dev/null || echo "  (user exists)"

CLOUD_SQL_INSTANCE="$PROJECT_ID:$REGION:$DB_INSTANCE"
echo "  Cloud SQL Instance: $CLOUD_SQL_INSTANCE"

# --- Step 4: Store DB password in Secret Manager ---
echo "[4/6] Storing secrets..."
echo -n "$DB_PASSWORD" | gcloud secrets create gatewell-db-password \
  --data-file=- --quiet 2>/dev/null || \
echo -n "$DB_PASSWORD" | gcloud secrets versions add gatewell-db-password \
  --data-file=- --quiet

# --- Step 5: Build & deploy backend ---
echo "[5/6] Building and deploying backend..."
BACKEND_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/gatewell/backend:latest"

gcloud builds submit --tag "$BACKEND_IMAGE" --quiet .

gcloud run deploy gatewell-api \
  --image "$BACKEND_IMAGE" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 3 \
  --add-cloudsql-instances "$CLOUD_SQL_INSTANCE" \
  --set-env-vars "SPRING_PROFILES_ACTIVE=cloudrun" \
  --set-env-vars "CLOUD_SQL_INSTANCE=$CLOUD_SQL_INSTANCE" \
  --set-env-vars "DB_NAME=$DB_NAME" \
  --set-env-vars "DB_USER=$DB_USER" \
  --set-secrets "DB_PASSWORD=gatewell-db-password:latest" \
  --quiet

BACKEND_URL=$(gcloud run services describe gatewell-api --region "$REGION" --format='value(status.url)')
echo "  Backend URL: $BACKEND_URL"

# Update CORS with actual backend URL
gcloud run services update gatewell-api \
  --region "$REGION" \
  --update-env-vars "FRONTEND_URL=https://gatewell-frontend-$(echo $PROJECT_ID | tr ':' '-').run.app" \
  --quiet

# --- Step 6: Build & deploy frontend ---
echo "[6/6] Building and deploying frontend..."
FRONTEND_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/gatewell/frontend:latest"

gcloud builds submit --tag "$FRONTEND_IMAGE" \
  --build-arg "NEXT_PUBLIC_API_URL=$BACKEND_URL" \
  ./frontend --quiet

gcloud run deploy gatewell-frontend \
  --image "$FRONTEND_IMAGE" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --memory 256Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 3 \
  --quiet

FRONTEND_URL=$(gcloud run services describe gatewell-frontend --region "$REGION" --format='value(status.url)')

# Update backend CORS with actual frontend URL
gcloud run services update gatewell-api \
  --region "$REGION" \
  --update-env-vars "FRONTEND_URL=$FRONTEND_URL" \
  --quiet

echo ""
echo "=== Deployment Complete ==="
echo "Frontend: $FRONTEND_URL"
echo "Backend:  $BACKEND_URL"
echo "Swagger:  $BACKEND_URL/swagger-ui/index.html"
echo ""
echo "DB Password (save this!): $DB_PASSWORD"
echo ""
echo "To create an API key:"
echo "  curl -X POST $BACKEND_URL/api-keys -H 'Content-Type: application/json' -d '{\"owner\":\"admin\"}'"
