# GCP 배포 작업 기록

**작업일:** 2026-04-21
**작업자:** Sunwoo Han
**프로젝트 ID:** project-c0638ffd-cf77-408e-897
**리전:** asia-northeast3 (서울)

---

## 1. gcloud CLI 설치 및 인증

```bash
brew install google-cloud-sdk
gcloud auth login
gcloud config set project project-c0638ffd-cf77-408e-897
```

- 설치 중 Python 3.9 경고 발생 → 동작에는 문제 없음
- 로그인: sunwoohan.me@gmail.com
- 무료 크레딧: ₩453,008 (2026-07-06 만료)

---

## 2. deploy/setup-gcp.sh 실행

### [1/6] GCP API 활성화 — 성공

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com
```

### [2/6] Artifact Registry 생성 — 성공

Docker 이미지 저장소 `gatewell` 생성 완료.

### [3/6] Cloud SQL 생성 — 성공

```
인스턴스: gatewell-db
버전: POSTGRES_15
사양: db-f1-micro
IP: 34.64.173.30
상태: RUNNABLE
```

- DB `gatewell` 생성 완료
- 유저 `gatewell` 생성 완료
- 약 5분 소요

### [4/6] Secret Manager — 성공

DB 비밀번호를 `gatewell-db-password` 시크릿에 저장 완료.

### [5/6] 백엔드 빌드 및 배포 — 이슈 3건 발생

#### 이슈 1: Cloud Build Storage 권한 오류

```
ERROR: Permission 'storage.objects.get' denied on resource
```

**원인:** Cloud Build 서비스 계정(937862530696-compute@developer.gserviceaccount.com)에 Storage 접근 권한 없음.

**해결:**
```bash
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/storage.admin"

gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/cloudbuild.builds.builder"

gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"
```

→ 빌드 재시도 후 성공 (빌드 시간: 4분 4초)

#### 이슈 2: Secret Manager 접근 권한 오류

```
ERROR: Permission denied on secret: projects/937862530696/secrets/gatewell-db-password
The service account must be granted 'Secret Manager Secret Accessor' role
```

**원인:** Cloud Run 서비스 계정에 Secret Manager 읽기 권한 없음.

**해결:**
```bash
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### 이슈 3: Cloud SQL 연결 권한 오류

```
ERROR: NOT_AUTHORIZED: Not authorized to access resource.
Possibly missing permission cloudsql.instances.get
```

**원인:** Cloud Run 서비스 계정에 Cloud SQL Client 역할 없음.

**해결:**
```bash
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

→ 최종 배포 성공

**백엔드 URL:** https://gatewell-api-937862530696.asia-northeast3.run.app

### [6/6] 프론트엔드 빌드 및 배포

#### 이슈 4: gcloud builds submit --build-arg 미지원

```
ERROR: unrecognized arguments: --build-arg
```

**원인:** `gcloud builds submit`은 Docker의 `--build-arg`를 직접 지원하지 않음.

**해결:** Dockerfile의 `ARG NEXT_PUBLIC_API_URL` 기본값을 배포된 백엔드 URL로 직접 수정 후 빌드.

```dockerfile
# 변경 전
ARG NEXT_PUBLIC_API_URL=http://localhost:8080

# 변경 후
ARG NEXT_PUBLIC_API_URL=https://gatewell-api-937862530696.asia-northeast3.run.app
```

→ 빌드 성공 (빌드 시간: 1분 34초), 배포 성공

**프론트엔드 URL:** https://gatewell-frontend-937862530696.asia-northeast3.run.app

### CORS 업데이트

백엔드의 `FRONTEND_URL` 환경변수를 프론트엔드 URL로 업데이트:

```bash
gcloud run services update gatewell-api \
  --region asia-northeast3 \
  --update-env-vars "FRONTEND_URL=https://gatewell-frontend-937862530696.asia-northeast3.run.app"
```

---

## 3. API Key 인증 이슈

#### 이슈 5: /api-keys 엔드포인트 인증 필요

배포 후 API 키를 생성하려 했으나, `/api-keys` 경로도 인증이 필요한 상태.

**해결:** `ApiKeyFilter.kt`의 `PUBLIC_PATHS`에 `/api-keys` 추가 후 재배포.

---

## 4. 도메인 등록 (gatewell.dev)

### Cloud Domains API 활성화

```bash
gcloud services enable domains.googleapis.com
```

### 도메인 검색

```bash
gcloud domains registrations search-domains gatewell.dev
# → AVAILABLE, $12.00/년
```

- `gatewell.com`은 1999년부터 GoDaddy에서 등록된 상태 (UNAVAILABLE)

### 등록 시도

#### 이슈 6: contact privacy 타입 오류

```
ERROR: Domain "gatewell.dev" does not support contact privacy type PRIVATE_CONTACT_DATA
```

**원인:** `.dev` 도메인은 `PRIVATE_CONTACT_DATA`를 지원하지 않음.

**해결:** `--contact-privacy=redacted-contact-data`로 변경.

```bash
gcloud domains registrations register gatewell.dev \
  --contact-data-from-file=/tmp/gatewell-contact.yaml \
  --contact-privacy=redacted-contact-data \
  --cloud-dns-zone=gatewell-zone \
  --yearly-price="12.00 USD" \
  --notices=hsts-preloaded
```

→ 등록 성공, 상태: ACTIVE

### 도메인 연결 시도

#### 이슈 7: 서울 리전에서 Cloud Run 도메인 매핑 미지원

```
ERROR: Creating domain mappings is not allowed in asia-northeast3
```

**원인:** Cloud Run의 커스텀 도메인 매핑은 `asia-northeast3` (서울) 리전에서 지원되지 않음.

**대안:**
- A안: 현재 `.run.app` URL로 운영 (추가 비용 없음) ← **채택**
- B안: Global External Load Balancer 사용 (월 ~$18-20 추가)

**결정:** 도메인은 확보 완료. 향후 Load Balancer 도입 시 연결 예정.

---

## 5. 추가 작업

### Scan findings 정렬

- URL/GitHub 스캔 결과를 심각도 순 정렬로 변경 (CRITICAL → HIGH → MEDIUM → LOW → INFO)
- 백엔드 수정 후 재배포 완료

### 프론트엔드 정리

- Assess 페이지, Dashboard를 네비게이션에서 제거
- 랜딩 페이지를 Scan 기능 중심으로 업데이트
- 재배포 완료

---

## 최종 배포 상태

| 항목 | 값 |
|------|-----|
| 프론트엔드 | https://gatewell-frontend-937862530696.asia-northeast3.run.app |
| 백엔드 API | https://gatewell-api-937862530696.asia-northeast3.run.app |
| Swagger 문서 | https://gatewell-api-937862530696.asia-northeast3.run.app/swagger-ui/index.html |
| 도메인 | gatewell.dev (등록 완료, 연결 미완 — 서울 리전 제한) |
| DB | Cloud SQL PostgreSQL 15 (gatewell-db, db-f1-micro) |
| 리전 | asia-northeast3 (서울) |

## 부여한 IAM 역할 목록

| 서비스 계정 | 역할 |
|------------|------|
| 937862530696-compute@developer.gserviceaccount.com | roles/storage.admin |
| 937862530696-compute@developer.gserviceaccount.com | roles/cloudbuild.builds.builder |
| 937862530696-compute@developer.gserviceaccount.com | roles/artifactregistry.writer |
| 937862530696-compute@developer.gserviceaccount.com | roles/secretmanager.secretAccessor |
| 937862530696-compute@developer.gserviceaccount.com | roles/cloudsql.client |

## 예상 월 비용

| 서비스 | 예상 비용 |
|--------|----------|
| Cloud SQL (db-f1-micro) | ~$7-10 |
| Cloud Run (유휴 시 0) | ~$0-2 |
| Artifact Registry | ~$0.1 |
| Cloud DNS | ~$0.2 |
| 도메인 (gatewell.dev) | $12/년 ($1/월) |
| **합계** | **~$8-13/월** |

## 참고: 리소스 정리 명령어

```bash
# Cloud SQL 일시 중지 (비용 절약)
gcloud sql instances patch gatewell-db --activation-policy=NEVER

# Cloud SQL 재시작
gcloud sql instances patch gatewell-db --activation-policy=ALWAYS

# 전체 삭제
./deploy/teardown-gcp.sh
```
