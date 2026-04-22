# GCP 배포 작업 기록

**작업일:** 2026-04-21 ~ 2026-04-22
**작업자:** Sunwoo Han (sunwoohan.me@gmail.com)
**프로젝트 ID:** project-c0638ffd-cf77-408e-897
**프로젝트 번호:** 937862530696
**리전:** asia-northeast1 (도쿄) — 서울에서 이전, 커스텀 도메인 매핑 지원
**서비스 계정:** 937862530696-compute@developer.gserviceaccount.com
**도메인:** gatewell.dev

---

## 사전 준비

### GCP 계정 상태

- 무료 체험판 사용 중
- 크레딧 잔액: ₩453,008 / ₩453,008 (사용량 ₩0)
- 만료일: 2026년 7월 6일

### gcloud CLI 설치

```bash
brew install google-cloud-sdk
```

설치 중 발생한 경고:
```
WARNING: Python 3.9.x is no longer officially supported by the Google Cloud CLI
```
→ 동작에는 문제없음. Python 3.10+ 사용을 권장하는 안내 메시지.

설치 후 PATH 설정 안내가 출력됨:
```
export PATH=/opt/homebrew/share/google-cloud-sdk/bin:"$PATH"
```

### 인증 및 프로젝트 설정

```bash
gcloud auth login
# → 브라우저가 열리고 Google 계정 로그인
# → "You are now logged in as [sunwoohan.me@gmail.com]."

gcloud config set project project-c0638ffd-cf77-408e-897
# → "Updated property [core/project]."
```

결제 계정 확인:
```bash
gcloud billing accounts list
# ACCOUNT_ID: 0163C3-09EBE1-141DBC
# NAME: 내 결제 계정
# OPEN: True
```

---

## 1단계: deploy/setup-gcp.sh 실행

스크립트 위치: `/deploy/setup-gcp.sh`
스크립트는 6단계로 구성되어 있으며, 5단계에서 권한 문제로 실패하여 수동으로 이어서 진행함.

### [1/6] GCP API 활성화 — 성공

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com
```

출력:
```
Operation "operations/acf.p2-937862530696-a4dadf65-d5a4-4afa-ae58-faf43a5b6962" finished successfully.
```

활성화된 API 목록:
| API | 용도 |
|-----|------|
| Cloud Run | 컨테이너 서버리스 배포 |
| Cloud SQL Admin | PostgreSQL 인스턴스 관리 |
| Cloud Build | Docker 이미지 빌드 |
| Artifact Registry | Docker 이미지 저장소 |
| Secret Manager | DB 비밀번호 등 시크릿 관리 |

### [2/6] Artifact Registry 생성 — 성공

```bash
gcloud artifacts repositories create gatewell \
  --repository-format=docker \
  --location=asia-northeast3
```

Docker 이미지를 저장할 저장소 `gatewell`이 `asia-northeast3` 리전에 생성됨.

### [3/6] Cloud SQL PostgreSQL 인스턴스 생성 — 성공

```bash
gcloud sql instances create gatewell-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=asia-northeast3 \
  --storage-type=HDD \
  --storage-size=10GB
```

소요 시간: 약 5분

생성 결과:
```
NAME         DATABASE_VERSION  LOCATION           TIER         PRIMARY_ADDRESS  STATUS
gatewell-db  POSTGRES_15       asia-northeast3-a  db-f1-micro  34.64.173.30     RUNNABLE
```

이어서 데이터베이스와 유저 생성:
```bash
gcloud sql databases create gatewell --instance=gatewell-db
gcloud sql users create gatewell --instance=gatewell-db --password=[자동생성]
```

Cloud SQL 인스턴스 연결 문자열:
```
project-c0638ffd-cf77-408e-897:asia-northeast3:gatewell-db
```

**참고:** `db-f1-micro`는 공유 vCPU + 614MB RAM 사양. 무료 체험 크레딧에서 차감되며, 월 약 $7-10 예상.

### [4/6] Secret Manager에 DB 비밀번호 저장 — 성공

```bash
echo -n "$DB_PASSWORD" | gcloud secrets create gatewell-db-password --data-file=-
```

비밀번호는 `openssl rand -base64 24`로 자동 생성되어 Secret Manager에 저장됨.
시크릿 이름: `gatewell-db-password`

### [5/6] 백엔드 빌드 및 배포 — 이슈 3건 발생

#### 이슈 1: Cloud Build Storage 권한 오류

**발생 시점:** `gcloud builds submit` 실행 시

**에러 메시지:**
```
ERROR: (gcloud.builds.submit) INVALID_ARGUMENT: could not resolve source:
googleapi: Error 403: 937862530696-compute@developer.gserviceaccount.com
does not have storage.objects.get access to the Google Cloud Storage object.
Permission 'storage.objects.get' denied on resource (or it may not exist)., forbidden
```

**원인:** Cloud Build는 소스코드를 GCS(Google Cloud Storage) 버킷에 업로드한 뒤 빌드하는데, 기본 서비스 계정에 Storage 접근 권한이 없었음. 새 프로젝트에서는 이 권한이 자동으로 부여되지 않는 경우가 있음.

**해결:**
```bash
# Storage 전체 관리 권한
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/storage.admin"

# Cloud Build 빌드 실행 권한
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/cloudbuild.builds.builder"

# Artifact Registry 쓰기 권한 (빌드된 이미지 push용)
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"
```

→ 권한 부여 후 빌드 재시도, 성공 (빌드 시간: 4분 4초)

빌드된 이미지:
```
asia-northeast3-docker.pkg.dev/project-c0638ffd-cf77-408e-897/gatewell/backend:latest
```

#### 이슈 2: Secret Manager 접근 권한 오류

**발생 시점:** `gcloud run deploy` 실행 후, Cloud Run이 시크릿을 읽으려 할 때

**에러 메시지:**
```
ERROR: spec.template.spec.containers[0].env[4].value_from.secret_key_ref.name:
Permission denied on secret: projects/937862530696/secrets/gatewell-db-password/versions/latest
for Revision service account 937862530696-compute@developer.gserviceaccount.com.
The service account used must be granted the 'Secret Manager Secret Accessor' role
```

**원인:** Cloud Run의 서비스 계정이 Secret Manager에서 시크릿 값을 읽을 권한이 없음. `--set-secrets` 옵션을 사용하면 Cloud Run이 런타임에 시크릿을 환경변수로 주입하는데, 이때 읽기 권한이 필요.

**해결:**
```bash
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### 이슈 3: Cloud SQL 연결 권한 오류

**발생 시점:** Cloud Run 컨테이너 시작 시 (Spring Boot → Cloud SQL 연결 시도)

**에러 메시지 (Cloud Run 로그에서 확인):**
```
com.google.api.client.googleapis.json.GoogleJsonResponseException
"status": "PERMISSION_DENIED"
"message": "boss::NOT_AUTHORIZED: Not authorized to access resource.
Possibly missing permission cloudsql.instances.get on resource instances/gatewell-db."
```

**발생 과정:**
1. Cloud Run 컨테이너가 시작됨
2. Spring Boot가 Cloud SQL에 소켓으로 연결 시도
3. `postgres-socket-factory`가 Cloud SQL Admin API를 통해 인스턴스 정보를 조회하려 함
4. 서비스 계정에 Cloud SQL Client 역할이 없어서 403 발생
5. DB 연결 실패 → Spring Boot 시작 실패 → Cloud Run 헬스체크 실패
6. `The user-provided container failed to start and listen on the port` 에러

**로그 확인 방법:**
```bash
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=gatewell-api" \
  --limit 20 --format="value(textPayload)"
```

**해결:**
```bash
gcloud projects add-iam-policy-binding project-c0638ffd-cf77-408e-897 \
  --member="serviceAccount:937862530696-compute@developer.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

→ 권한 부여 후 재배포, Cloud Run 서비스 정상 시작

**최종 배포 성공:**
```
Service [gatewell-api] revision [gatewell-api-00003-rb8] has been deployed
and is serving 100 percent of traffic.
Service URL: https://gatewell-api-937862530696.asia-northeast3.run.app
```

**동작 확인:**
```bash
curl -s https://gatewell-api-937862530696.asia-northeast3.run.app/rule-version
# → {"version":"1.0.0","totalRules":11,"rules":[...]}
```

**실제 스캔 테스트:**
```bash
curl -s -X POST https://gatewell-api-937862530696.asia-northeast3.run.app/scan/url \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org"}'
# → decision: BLOCK, findings: 8건 (CRITICAL 1, HIGH 2, MEDIUM 4, LOW 1)
```

### [6/6] 프론트엔드 빌드 및 배포

#### 이슈 4: gcloud builds submit --build-arg 미지원

**발생 시점:** 프론트엔드 Docker 이미지 빌드 시

**에러 메시지:**
```
ERROR: (gcloud.builds.submit) unrecognized arguments: --build-arg
```

**원인:** `gcloud builds submit --tag` 명령은 간편 빌드 모드로, Docker의 `--build-arg` 옵션을 직접 전달할 수 없음. `--build-arg`를 사용하려면 `cloudbuild.yaml` 설정 파일을 작성해서 `docker build` 단계에서 명시적으로 전달해야 함.

**해결 (간편 방법):** Dockerfile의 `ARG` 기본값을 직접 수정

```dockerfile
# 변경 전
ARG NEXT_PUBLIC_API_URL=http://localhost:8080

# 변경 후
ARG NEXT_PUBLIC_API_URL=https://gatewell-api-937862530696.asia-northeast3.run.app
```

**참고:** `NEXT_PUBLIC_*` 환경변수는 Next.js 빌드 타임에 번들에 하드코딩됨. 런타임 환경변수로는 전달 불가. 따라서 빌드 시점에 올바른 API URL이 포함되어야 함.

```bash
gcloud builds submit --tag \
  asia-northeast3-docker.pkg.dev/project-c0638ffd-cf77-408e-897/gatewell/frontend:latest \
  ./frontend
```

→ 빌드 성공 (빌드 시간: 1분 34초)

**프론트엔드 배포:**
```bash
gcloud run deploy gatewell-frontend \
  --image asia-northeast3-docker.pkg.dev/project-c0638ffd-cf77-408e-897/gatewell/frontend:latest \
  --region asia-northeast3 \
  --platform managed \
  --allow-unauthenticated \
  --memory 256Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 3
```

```
Service [gatewell-frontend] revision [gatewell-frontend-00001-zdt] has been deployed
and is serving 100 percent of traffic.
Service URL: https://gatewell-frontend-937862530696.asia-northeast3.run.app
```

### CORS 업데이트

프론트엔드 URL이 확정된 후, 백엔드의 CORS 설정을 업데이트:

```bash
gcloud run services update gatewell-api \
  --region asia-northeast3 \
  --update-env-vars "FRONTEND_URL=https://gatewell-frontend-937862530696.asia-northeast3.run.app"
```

이 작업으로 새 리비전(gatewell-api-00004-zr8)이 생성되고 트래픽이 전환됨.

---

## 2단계: API Key 설정

### 이슈 5: /api-keys 엔드포인트 인증 필요

**발생 시점:** 배포 후 최초 API 키 생성 시도 시

**에러:**
```bash
curl -X POST https://gatewell-api-.../api-keys \
  -H "Content-Type: application/json" \
  -d '{"owner": "admin"}'
# → {"status":401,"error":"Unauthorized"}
```

**원인:** prod 프로필에서 `gatewell.auth.enabled=true`이므로 모든 요청에 API 키가 필요. 그런데 API 키를 생성하는 엔드포인트 자체도 인증 대상이라 최초 키 생성이 불가능한 순환 문제.

**해결:** `ApiKeyFilter.kt`의 `PUBLIC_PATHS`에 `/api-keys` 추가:

```kotlin
private val PUBLIC_PATHS = setOf(
    "/rule-version",
    "/api-keys",    // ← 추가
    "/scan",
    "/h2-console",
    "/swagger-ui",
    "/v3/api-docs",
    "/actuator",
)
```

→ 백엔드 재빌드 및 재배포 후 API 키 생성 성공

**참고:** 프로덕션에서는 `/api-keys` 엔드포인트에 별도의 관리자 인증을 추가해야 함. 현재는 누구나 키를 생성할 수 있는 상태.

---

## 3단계: 도메인 등록 (gatewell.dev)

### Cloud Domains API 활성화

```bash
gcloud services enable domains.googleapis.com
```

### 도메인 검색

```bash
gcloud domains registrations search-domains gatewell.dev
```

결과:
```
DOMAIN        AVAILABILITY  YEARLY_PRICE  NOTICES
gatewell.dev  AVAILABLE     12.00 USD     HSTS_PRELOADED
gatewell.app  AVAILABLE     14.00 USD     HSTS_PRELOADED
gatewell.com  UNAVAILABLE
gatewell.io   AVAILABLE     60.00 USD
gatewell.net  AVAILABLE     12.00 USD
```

- `gatewell.com`은 1999년부터 GoDaddy에서 등록된 상태 (UNAVAILABLE)
- `gatewell.dev`를 선택 ($12/년, GCP 크레딧에서 차감)
- `HSTS_PRELOADED`: .dev 도메인은 브라우저에서 강제 HTTPS

### Cloud DNS Zone 생성

도메인 등록 전에 DNS zone이 필요:
```bash
gcloud services enable dns.googleapis.com

gcloud dns managed-zones create gatewell-zone \
  --dns-name="gatewell.dev." \
  --description="Gatewell DNS zone" \
  --visibility=public
```

### 등록자 연락처 정보 작성

`/tmp/gatewell-contact.yaml`:
```yaml
allContacts:
  email: "sunwoohan.me@gmail.com"
  phoneNumber: "+82.1051649165"
  postalAddress:
    regionCode: "KR"
    postalCode: "04524"
    administrativeArea: "Seoul"
    locality: "Seoul"
    addressLines:
      - "123 Gangnam-daero, Seocho-gu"
    recipients:
      - "Sunwoo Han"
```

WHOIS에서 `redacted-contact-data` 옵션으로 개인정보 비공개 처리.

### 이슈 6: contact privacy 타입 오류

**에러 메시지:**
```
ERROR: Domain "gatewell.dev" does not support contact privacy type PRIVATE_CONTACT_DATA
```

**원인:** 도메인 TLD마다 지원하는 privacy 옵션이 다름. `.dev` 도메인의 지원 옵션 확인:

```bash
gcloud domains registrations get-register-parameters gatewell.dev
# supportedPrivacy:
# - PUBLIC_CONTACT_DATA
# - REDACTED_CONTACT_DATA
```

`.dev`는 `PRIVATE_CONTACT_DATA`를 지원하지 않고, `REDACTED_CONTACT_DATA`만 지원.
- `PRIVATE_CONTACT_DATA`: 프록시 연락처로 대체 (완전 비공개)
- `REDACTED_CONTACT_DATA`: 실제 정보 중 일부를 가림 (부분 비공개)

**해결:** `--contact-privacy=redacted-contact-data`로 변경

### 최종 도메인 등록

```bash
gcloud domains registrations register gatewell.dev \
  --contact-data-from-file=/tmp/gatewell-contact.yaml \
  --contact-privacy=redacted-contact-data \
  --cloud-dns-zone=gatewell-zone \
  --yearly-price="12.00 USD" \
  --notices=hsts-preloaded
```

출력:
```
Created registration [gatewell.dev]
Note: The domain is not yet registered.
Wait until the registration resource changes state to ACTIVE.
```

상태 확인:
```bash
gcloud domains registrations describe gatewell.dev
# state: ACTIVE
```

→ 등록 완료. GCP 크레딧에서 $12 차감.

---

## 4단계: 커스텀 도메인 연결 시도

### 이슈 7: 서울 리전(asia-northeast3)에서 Cloud Run 도메인 매핑 미지원

**시도한 명령:**
```bash
gcloud beta run domain-mappings create \
  --service gatewell-frontend \
  --domain gatewell.dev \
  --region asia-northeast3
```

**에러 메시지:**
```json
{
  "error": {
    "code": 501,
    "message": "Creating domain mappings is not allowed in asia-northeast3.",
    "status": "UNIMPLEMENTED"
  }
}
```

**원인:** Cloud Run의 커스텀 도메인 매핑 기능은 모든 리전에서 지원되지 않음. `asia-northeast3` (서울)은 미지원 리전.

**지원 리전 확인:** 도쿄 리전(asia-northeast1)에서는 도메인 매핑이 지원됨:
```bash
gcloud beta run domain-mappings list --region asia-northeast1
# → Listed 0 items.  (에러 없이 응답 = 지원)
```

### 대안 분석

| 방안 | 설명 | 비용 | 복잡도 |
|------|------|------|--------|
| A. 현재 .run.app URL 유지 | 도메인은 확보해두고 나중에 연결 | 없음 | 없음 |
| B. 도쿄 리전으로 재배포 | asia-northeast1에서 도메인 매핑 지원 | 없음 (리전 이동만) | 중간 |
| C. Load Balancer 사용 | Global LB → Cloud Run 연결 | 월 ~$18-20 | 높음 |

### 결정 및 실행

**B안 채택** — 도쿄 리전(asia-northeast1)으로 전체 재배포 완료 (2026-04-21)

실행한 작업:
1. Artifact Registry를 도쿄에 생성
2. Cloud SQL `gatewell-db-tokyo` 인스턴스를 도쿄에 생성
3. 백엔드/프론트엔드를 도쿄 Cloud Run에 배포
4. `gcloud beta run domain-mappings create --domain gatewell.dev --region asia-northeast1` → 성공
5. `gcloud beta run domain-mappings create --domain api.gatewell.dev --region asia-northeast1` → 성공
6. Cloud DNS에 A/AAAA/CNAME 레코드 추가
7. 서울 리전 리소스 전체 삭제 (Cloud Run, Cloud SQL, Artifact Registry)

---

## 5단계: 추가 작업

### Scan findings 정렬

스캔 결과가 심각도 순서 없이 반환되던 문제 수정.

**변경 내용:** `UrlSecurityScanner.kt`, `GitHubCodeScanner.kt`에서 findings를 `ScanSeverity.ordinal` 기준 정렬:
```kotlin
// 변경 전
return ScanReport(url = url, reachable = true, findings = findings.toList())

// 변경 후
return ScanReport(url = url, reachable = true, findings = findings.sortedBy { it.severity.ordinal })
```

정렬 순서: CRITICAL → HIGH → MEDIUM → LOW → INFO

→ 백엔드 재빌드 및 재배포 완료 (리비전: gatewell-api-00005-6fx)

### 프론트엔드 정리

- 네비게이션에서 Assess, Dashboard 링크 제거
- 랜딩 페이지 문구를 Scan 기능 중심으로 업데이트
- CTA 버튼: "Try Free Assessment" → "Scan Now — Free"
- 기능 소개: 실제 보안 스캔 기능 위주로 변경

→ 프론트엔드 재빌드 및 재배포 완료 (리비전: gatewell-frontend-00002-4ts)

---

## 최종 배포 상태

| 항목 | 값 |
|------|-----|
| 프론트엔드 URL | https://gatewell.dev |
| 백엔드 API URL | https://api.gatewell.dev |
| Cloud Run 프론트엔드 | https://gatewell-frontend-937862530696.asia-northeast1.run.app |
| Cloud Run 백엔드 | https://gatewell-api-937862530696.asia-northeast1.run.app |
| Swagger 문서 | https://api.gatewell.dev/swagger-ui/index.html |
| 도메인 | gatewell.dev (등록 + 매핑 완료) |
| DB 인스턴스 | gatewell-db-tokyo (PostgreSQL 15, db-f1-micro) |
| 리전 | asia-northeast1 (도쿄) |

### Cloud Run 서비스 사양

| 서비스 | 메모리 | CPU | 최소 인스턴스 | 최대 인스턴스 |
|--------|-------|-----|-------------|-------------|
| gatewell-api | 512Mi | 1 | 0 | 3 |
| gatewell-frontend | 256Mi | 1 | 0 | 3 |

`min-instances=0`이므로 요청이 없으면 인스턴스가 0으로 줄어들어 비용이 발생하지 않음.
단, 콜드 스타트 시 백엔드(Spring Boot)는 첫 요청에 5-10초 소요될 수 있음.

### 부여한 IAM 역할 전체 목록

| 서비스 계정 | 역할 | 용도 |
|------------|------|------|
| 937862530696-compute@developer.gserviceaccount.com | roles/storage.admin | Cloud Build 소스 업로드 |
| 937862530696-compute@developer.gserviceaccount.com | roles/cloudbuild.builds.builder | Cloud Build 빌드 실행 |
| 937862530696-compute@developer.gserviceaccount.com | roles/artifactregistry.writer | Docker 이미지 push |
| 937862530696-compute@developer.gserviceaccount.com | roles/secretmanager.secretAccessor | DB 비밀번호 읽기 |
| 937862530696-compute@developer.gserviceaccount.com | roles/cloudsql.client | Cloud SQL 소켓 연결 |

### 예상 월 비용

| 서비스 | 예상 비용 | 비고 |
|--------|----------|------|
| Cloud SQL (db-f1-micro) | ~$7-10 | 항상 실행 중 (일시중지 가능) |
| Cloud Run (유휴 시 0) | ~$0-2 | min-instances=0, 요청 없으면 무료 |
| Artifact Registry | ~$0.1 | 이미지 저장 용량 기준 |
| Cloud DNS | ~$0.2 | zone 유지비 |
| 도메인 (gatewell.dev) | $1/월 | $12/년 |
| Cloud Build | 무료 | 일 120분 빌드 무료 |
| **합계** | **~$8-13/월** | 크레딧으로 25-30개월 사용 가능 |

---

## 이슈 요약

| # | 이슈 | 단계 | 원인 | 해결 |
|---|------|------|------|------|
| 1 | Storage 권한 오류 | 백엔드 빌드 | 서비스 계정에 GCS 접근 권한 없음 | roles/storage.admin 부여 |
| 2 | Secret Manager 접근 거부 | 백엔드 배포 | 시크릿 읽기 권한 없음 | roles/secretmanager.secretAccessor 부여 |
| 3 | Cloud SQL 연결 실패 | 백엔드 시작 | Cloud SQL Client 역할 없음 | roles/cloudsql.client 부여 |
| 4 | --build-arg 미지원 | 프론트엔드 빌드 | gcloud builds submit 간편 모드 제한 | Dockerfile ARG 기본값 직접 수정 |
| 5 | /api-keys 인증 순환 | API 키 생성 | 키 생성 엔드포인트도 인증 필요 | PUBLIC_PATHS에 /api-keys 추가 |
| 6 | privacy 타입 오류 | 도메인 등록 | .dev가 PRIVATE_CONTACT_DATA 미지원 | REDACTED_CONTACT_DATA로 변경 |
| 7 | 도메인 매핑 미지원 | 도메인 연결 | asia-northeast3 리전 제한 | 보류 (도쿄 리전 재배포로 해결 가능) |

---

## 리소스 관리 명령어

```bash
# Cloud SQL 일시 중지 (비용 절약, 사용하지 않을 때)
gcloud sql instances patch gatewell-db-tokyo --activation-policy=NEVER

# Cloud SQL 재시작
gcloud sql instances patch gatewell-db-tokyo --activation-policy=ALWAYS

# 배포된 서비스 상태 확인
gcloud run services list --region asia-northeast1

# Cloud Run 로그 확인
gcloud run services logs read gatewell-api --region asia-northeast1 --limit 50

# 도메인 매핑 상태 확인
gcloud beta run domain-mappings list --region asia-northeast1

# 도메인 상태 확인
gcloud domains registrations describe gatewell.dev

# 전체 리소스 삭제 (더 이상 사용하지 않을 때)
./deploy/teardown-gcp.sh
```

---

## 6단계: 보안 점검 및 수정 (2026-04-22)

### 수정된 보안 이슈

| # | 이슈 | 심각도 | 수정 내용 |
|---|------|--------|----------|
| 1 | SSRF 취약점 | HIGH | UrlSecurityScanner에 private IP, loopback, metadata endpoint 차단 추가 |
| 2 | /api-keys 무제한 공개 | CRITICAL | X-Admin-Secret 헤더 검증 추가, PUBLIC_PATHS에서 제거 |
| 3 | /actuator, /h2-console 공개 | CRITICAL | PUBLIC_PATHS에서 제거 |
| 4 | application-prod.yml 빈 패스워드 | CRITICAL | 기본값 제거, 환경변수 필수 |
| 5 | deploy 스크립트 패스워드 출력 | CRITICAL | echo 제거, Secret Manager 조회 안내로 대체 |
| 6 | CORS 와일드카드 | MEDIUM | 허용 헤더/메서드를 명시적으로 제한 |
| 7 | docker-compose 기본 패스워드 | HIGH | 기본값 제거, 환경변수 필수로 변경 |

### 파일 정리

- 삭제: frontend/public/{vercel,next,file,globe,window}.svg (템플릿 파일)
- 삭제: frontend/app/assess/, frontend/app/dashboard/ (미사용 페이지)
- 삭제: frontend/README.md, frontend/CLAUDE.md (기본 템플릿)
- 삭제: .DS_Store
- 삭제: 머지 완료된 feature 브랜치 7개
- 수정: frontend/Dockerfile ARG 기본값을 localhost로 복원 (프로젝트 ID 노출 방지)

---

## 향후 작업

1. **예산 알림 설정** — GCP 콘솔 → 결제 → 예산 만들기
2. **Cloud SQL 공개 IP 비활성화** — `gcloud sql instances patch gatewell-db-tokyo --no-assign-ip`
3. **ADMIN_SECRET 환경변수 설정** — Cloud Run에 시크릿으로 추가
4. **GitHub API 토큰 추가** — GitHubCodeScanner 인증 요청으로 rate limit 확보 (60→5000/hr)
5. **Cold start 최적화** — min-instances=1로 변경 시 항상 하나의 인스턴스 대기 (비용 증가)
