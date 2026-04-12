# Research

## 1. 전체 구조

현재 저장소는 실행 가능한 백엔드 서비스가 아니라, 백엔드 구현을 시작하기 위한 최소 골격과 작업 절차 문서가 있는 상태다.

루트 기준 실제 구성:

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `gradle/wrapper/*`
- `src/main/kotlin`
- `src/main/resources`
- `src/test/kotlin`
- `src/test/resources`
- `prompts/*.txt`
- `docs/research.md`
- `docs/plan.md`
- `AGENTS.md`
- `CODEX.md`

관찰 결과:

- Gradle 단일 모듈 프로젝트다.
- Kotlin JVM 설정만 존재한다.
- `src` 아래 실제 Kotlin 소스와 테스트 파일은 없다.
- `prompts/`는 단계별 작업 템플릿 역할을 한다.
- `AGENTS.md`는 제품 성격, 승인 규칙, 파이프라인, 아키텍처 기대사항을 함께 정의한다.

정리하면 현재 저장소의 실체는 "백엔드 구현 준비 상태"다.

## 2. 핵심 로직 흐름

현재 코드베이스에는 실행 가능한 애플리케이션 로직이 없다. 따라서 분석 가능한 흐름은 문서가 요구하는 목표 흐름과 작업 파이프라인뿐이다.

제품 목표 흐름:

1. 사용자가 assessment를 제출한다.
2. 입력을 검증한다.
3. risk engine이 리스크를 평가한다.
4. findings와 severity를 계산한다.
5. recommendation과 launch decision을 만든다.
6. 결과를 저장하고 report를 반환한다.

하네스 목표 흐름:

1. scenario를 로드한다.
2. 동일한 평가 로직을 실행한다.
3. expected와 actual을 비교한다.
4. regression result를 기록한다.

작업 파이프라인:

1. `research.txt`
2. `planning.txt`
3. `annotation.txt`
4. 승인 전까지 planning/annotation 반복
5. `tasks.txt`
6. `implementation.txt`
7. `review.txt`

현재 부재한 요소:

- 애플리케이션 엔트리포인트
- HTTP API
- 도메인 모델
- 평가 규칙 엔진
- persistence 계층
- harness 구현
- 테스트 코드

즉, 현재는 "의도된 동작"만 문서로 존재하고 "실행되는 동작"은 없다.

## 3. 사용 기술 및 패턴

실제 확인된 기술 스택:

- Gradle Wrapper `9.2.1`
- Kotlin JVM plugin `2.3.10`
- JVM toolchain `21`
- 테스트 의존성 `org.jetbrains.kotlin:kotlin-test`
- 저장소 `mavenCentral()`

실제 확인된 패턴:

- 표준 Gradle 디렉터리 구조
- `main` / `test` 분리
- 문서 중심 작업 절차
- 아직 프레임워크 의존성이 거의 없는 상태

문서가 요구하는 패턴:

- Kotlin + Spring Boot 백엔드
- domain / controller / persistence 분리
- deterministic한 평가 로직
- 재현 가능한 harness
- 사용자 승인 우선 작업 방식

현재와 문서 기대 사이 차이:

- `AGENTS.md`는 Spring Boot 기반 백엔드를 요구하지만, 실제 빌드는 순수 Kotlin JVM만 설정돼 있다.
- API, persistence, validation, harness 관련 의존성과 코드가 모두 없다.
- 아키텍처 기대사항은 명확하지만 그것을 지탱할 구현은 아직 없다.

## 4. 숨겨진 의존성 및 제약

현재 저장소에는 코드 수준 숨겨진 의존성은 거의 없지만, 문서 수준 제약은 강하다.

핵심 제약:

- 사용자 승인 없이 임의로 진행하면 안 된다.
- 파일 수정, 삭제, 생성, 명령 실행, 범위 확장, 아키텍처 변경은 명시적 승인 대상이다.
- 파이프라인 순서를 임의로 바꾸면 안 된다.
- harness는 선택 사항이 아니라 필수 품질 계층이다.

앞으로 추가될 가능성이 높은 의존성:

- Spring Boot plugin
- web starter
- validation starter
- persistence starter
- DB driver
- JSON 직렬화 설정
- Spring 테스트 인프라
- harness fixture 및 comparator 지원 코드

문서 간 충돌 가능성:

- `AGENTS.md`는 `.txt`를 작업 단계 템플릿으로 정의한다.
- `prompts/*.txt`는 결과물을 `docs/*.md`로 작성하도록 지시한다.
- 따라서 "단계 템플릿은 `.txt`, 실제 작업 결과는 `docs/*.md`"로 해석하면 현재 저장소와 가장 잘 맞는다.

## 5. 문제 가능성

1. 기술 기준과 실제 빌드가 어긋나 있다.

- 현재 `build.gradle`은 Spring Boot 없이 Kotlin JVM만 사용한다.
- 하지만 `AGENTS.md`는 Kotlin + Spring Boot 백엔드를 전제로 둔다.

2. 승인 규칙이 매우 강하다.

- 자동 진행을 전제로 한 작업 습관과 충돌할 수 있다.
- 작은 탐색이나 검증도 승인 범위로 해석될 여지가 있다.

3. 파이프라인은 명확하지만 산출물 정책은 완전히 고정되지 않았다.

- `.txt` 템플릿과 `docs/*.md` 결과물의 역할 분리가 암묵적이다.
- 정리 없이 진행하면 다시 문서가 혼합될 수 있다.

4. 구현체가 없어서 planning은 사실상 greenfield 설계가 된다.

- 코드 리뷰가 아니라 초기 구조 설계가 중심이 된다.

5. harness 요구 수준이 높다.

- MVP라도 단순 API만 만들고 끝내면 문서 요구를 충족하지 못한다.

## 6. 개선 포인트

1. 실제 빌드 설정을 목표 아키텍처와 맞춰야 한다.

- Spring Boot 도입
- validation / persistence / test stack 확정
- 패키지 구조와 엔트리포인트 추가

2. 첫 구현 범위를 더 명확히 잘라야 한다.

권장 최소 범위:

- 부팅 가능한 Spring Boot 앱
- `POST /assessments` 단일 API
- deterministic risk scoring
- launch decision 산출
- report 저장
- harness 기본 시나리오 1~2개

3. 문서 산출물 규칙을 일관되게 유지해야 한다.

- `prompts/*.txt`는 단계 템플릿
- `docs/*.md`는 현재 작업 결과
- 구현 완료 후 `.txt`는 커밋 제외

4. 설계 단계부터 harness를 포함해야 한다.

- 추후 추가가 아니라 초기 구조에 comparator, scenario, rule version 개념을 반영해야 한다.

## 결론

현재 프로젝트는 "출시 리스크 평가 백엔드"의 구현체가 아니라, 그 구현을 시작하기 위한 Kotlin/Gradle 골격과 엄격한 작업 절차가 있는 초기 상태다. 새 `AGENTS.md` 기준으로 보면 핵심은 세 가지다.

- 이 프로젝트는 백엔드 중심이다.
- 사용자의 명시적 승인이 최상위 제약이다.
- harness를 포함한 재현 가능한 평가 시스템을 만들어야 한다.

다음 planning 단계는 기존 문서의 오래된 전제를 이어받는 것이 아니라, 이 현재 상태를 기준으로 Spring Boot 기반 첫 vertical slice를 설계하는 작업이어야 한다.
