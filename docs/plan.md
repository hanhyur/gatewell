# Plan

## 전제와 가정

이 계획은 현재 저장소 상태와 업데이트된 `AGENTS.md`, `CODEX.md`를 기준으로 작성한다.

명시적 가정:

1. 현재 프로젝트는 greenfield 백엔드 상태다.
2. planning 단계에서는 구현하지 않고 구조와 범위를 고정하는 데 집중한다.
3. 새 `AGENTS.md`의 사용자 승인 규칙과 단계 파이프라인을 우선 적용한다.
4. 기술 방향은 Kotlin + Spring Boot 백엔드다.
5. 첫 구현은 단일 모듈 안에서 시작한다.
6. Querydsl 같은 확장 요소는 실제 조회 요구가 생기기 전까지 도입하지 않는다.
7. 구현은 TDD를 기본 진행 방식으로 삼는다.
8. 도메인 모델은 DDD 관점에서 entity / value object / policy를 분리한다.

가정이 틀릴 수 있는 지점:

- 운영 DB 종류가 정해져 있을 수 있다.
- persistence 저장 방식이 JPA가 아닐 수 있다.
- API 계약이 이미 외부에서 정해져 있을 수 있다.

이 가정들이 바뀌면 의존성, DTO, persistence 구조가 함께 바뀌어야 한다.

## 1. 접근 방식 상세 설명

접근 방식은 "최소한의 완전한 backend vertical slice"를 만드는 방향이 적절하다.

설계 원칙:

- 테스트를 먼저 작성하고 최소 구현으로 통과시킨다.
- 먼저 Spring Boot 앱이 정상 부팅되어야 한다.
- controller / application / domain / persistence를 분리한다.
- risk evaluation은 가능한 한 순수 도메인 로직으로 둔다.
- 요청/응답 DTO와 도메인 모델을 분리한다.
- primitive obsession을 피하고 도메인 의미가 있는 타입을 우선 사용한다.
- 첫 범위는 assessment 생성과 평가 결과 반환에 한정한다.
- persistence는 report 저장까지 포함하되 조회 API는 초기 범위에서 제외한다.
- harness는 초기부터 구조에 포함하되 시나리오는 최소 개수로 시작한다.

권장 패키지 구조:

- `me.hanhyur.gatewell`
- `me.hanhyur.gatewell.assessment.api`
- `me.hanhyur.gatewell.assessment.application`
- `me.hanhyur.gatewell.assessment.domain`
- `me.hanhyur.gatewell.assessment.domain.model`
- `me.hanhyur.gatewell.assessment.domain.policy`
- `me.hanhyur.gatewell.assessment.domain.port`
- `me.hanhyur.gatewell.assessment.infrastructure.persistence`
- `me.hanhyur.gatewell.common.api`
- `me.hanhyur.gatewell.harness`
- `me.hanhyur.gatewell.harness.domain`
- `me.hanhyur.gatewell.harness.application`

DDD 적용 기준:

- `Assessment`, `AssessmentReport`는 aggregate root 후보로 둔다.
- `ProductName`, `Evidence`, `FindingCode`, `RuleVersion`, `ScenarioId` 같은 값은 value object로 분리한다.
- launch decision 계산은 `LaunchDecisionPolicy` 같은 domain policy로 둔다.
- 저장은 domain port를 통해 의존하고 구현은 infrastructure에서 제공한다.

TDD 적용 기준:

1. domain rule 테스트 작성
2. decision policy 테스트 작성
3. harness comparator 테스트 작성
4. API integration 테스트 작성
5. 최소 구현 작성
6. 리팩터링

구현 순서 기준 핵심 흐름:

1. `POST /assessments` 요청 수신
2. 요청 DTO 검증
3. application service가 command 생성
4. risk engine이 findings와 severity 계산
5. decision policy가 recommendation과 launch decision 계산
6. report aggregate 생성
7. persistence 계층에 저장
8. 응답 DTO 반환

하네스 흐름:

1. 시나리오 fixture 로드
2. 동일 command 생성
3. 동일 risk engine / decision policy 실행
4. expected와 actual 비교
5. mismatch 보고

## 2. 변경 대상 파일 경로

planning 기준 변경 대상:

- `build.gradle`
- `src/main/kotlin/me/hanhyur/gatewell/GatewellApplication.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentController.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentRequest.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentResponse.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/application/AssessmentCommand.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/application/AssessmentService.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Assessment.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/AssessmentId.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/AssessmentReport.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Capability.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Evidence.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Finding.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/FindingCode.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/ProductName.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/RiskCategory.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/RuleVersion.kt`
- `src/main/kotlin/me/hanhyur/gatewell/harness/domain/ScenarioId.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Severity.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Recommendation.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/LaunchDecision.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/ScoringResult.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/policy/RiskScoringEngine.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/policy/LaunchDecisionPolicy.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/port/AssessmentReportStore.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/infrastructure/persistence/AssessmentReportEntity.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/infrastructure/persistence/JpaAssessmentReportRepository.kt`
- `src/main/kotlin/me/hanhyur/gatewell/assessment/infrastructure/persistence/AssessmentReportStoreImpl.kt`
- `src/main/kotlin/me/hanhyur/gatewell/common/api/ApiErrorResponse.kt`
- `src/main/kotlin/me/hanhyur/gatewell/common/api/GlobalExceptionHandler.kt`
- `src/main/resources/application.yml`
- `src/test/kotlin/me/hanhyur/gatewell/assessment/domain/model/SeverityTest.kt`
- `src/test/kotlin/me/hanhyur/gatewell/assessment/domain/policy/RiskScoringEngineTest.kt`
- `src/test/kotlin/me/hanhyur/gatewell/assessment/domain/policy/LaunchDecisionPolicyTest.kt`
- `src/test/kotlin/me/hanhyur/gatewell/harness/domain/HarnessComparatorTest.kt`
- `src/test/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentApiIntegrationTest.kt`
- `src/test/kotlin/me/hanhyur/gatewell/harness/AssessmentHarnessTest.kt`
- `src/test/resources/harness/prompt-injection-basic.json`
- `src/test/resources/harness/missing-rate-limit.json`

## 3. 코드 스니펫

애플리케이션 엔트리포인트 예시:

```kotlin
@SpringBootApplication
class GatewellApplication

fun main(args: Array<String>) {
    runApplication<GatewellApplication>(*args)
}
```

API 요청 DTO 예시:

```kotlin
data class AssessmentRequest(
    @field:NotBlank
    val productName: String,
    @field:NotBlank
    val summary: String,
    @field:NotEmpty
    val evidences: List<String>,
    val capabilities: List<String> = emptyList()
)
```

도메인 타입 예시:

```kotlin
@JvmInline
value class ProductName(val value: String) {
    init {
        require(value.isNotBlank()) { "productName must not be blank" }
    }
}

@JvmInline
value class RuleVersion(val value: String)

@JvmInline
value class FindingCode(val value: String)
```

application command 예시:

```kotlin
data class AssessmentCommand(
    val productName: ProductName,
    val summary: String,
    val evidences: List<Evidence>,
    val capabilities: Set<Capability>,
    val ruleVersion: RuleVersion,
)
```

도메인 저장 포트 예시:

```kotlin
interface AssessmentReportStore {
    fun save(report: AssessmentReport): AssessmentReport
}
```

서비스 흐름 예시:

```kotlin
@Service
class AssessmentService(
    private val riskScoringEngine: RiskScoringEngine,
    private val launchDecisionPolicy: LaunchDecisionPolicy,
    private val assessmentReportStore: AssessmentReportStore,
) {
    fun assess(command: AssessmentCommand): AssessmentReport {
        val scoringResult = riskScoringEngine.evaluate(command)
        val decisionResult = launchDecisionPolicy.decide(scoringResult)
        val report = AssessmentReport.create(command, scoringResult, decisionResult)
        return assessmentReportStore.save(report)
    }
}
```

도메인 규칙 예시:

```kotlin
class RiskScoringEngine {
    fun evaluate(command: AssessmentCommand): ScoringResult {
        val findings = mutableListOf<Finding>()

        if (Capability.CODE_EXECUTION in command.capabilities) {
            findings += Finding.high(
                category = RiskCategory.AUTH_WEAKNESS,
                code = FindingCode("CAPABILITY_CODE_EXECUTION"),
                message = "Code execution raises launch risk without strong isolation evidence."
            )
        }

        val severity = Severity.from(findings)
        return ScoringResult(findings, severity)
    }
}
```

하네스 비교 결과 예시:

```kotlin
data class HarnessResult(
    val scenarioId: String,
    val expectedDecision: LaunchDecision,
    val actualDecision: LaunchDecision,
    val passed: Boolean,
    val mismatches: List<String>,
)
```

## 4. 데이터 흐름

TDD 흐름:

1. 실패하는 domain test 작성
2. 최소 domain 구현으로 통과
3. policy test 추가
4. harness comparator test 추가
5. API integration test 추가
6. 필요한 인프라 구현
7. 중복 제거와 타입 정제

쓰기 흐름:

1. 클라이언트가 assessment 요청을 보낸다.
2. controller가 요청을 검증한다.
3. request DTO를 domain-aware command로 변환한다.
4. application service가 aggregate 생성을 시작한다.
5. risk engine이 finding과 severity를 계산한다.
6. decision policy가 recommendation과 launch decision을 계산한다.
7. application service가 report aggregate를 조합한다.
8. domain port를 통해 저장한다.
9. infrastructure가 entity로 변환해 persistence에 기록한다.
10. response DTO로 반환한다.

도메인 내부 흐름:

- raw input을 의미 있는 타입으로 변환
- 규칙 매칭
- finding 생성
- severity 집계
- decision 계산
- recommendation 생성
- evidence와 함께 report 조합

하네스 흐름:

1. fixture 로드
2. fixture를 domain type으로 변환
3. 평가 실행
4. decision / severity / categories / key findings 비교
5. pass 여부와 mismatch 반환

## 5. 트레이드오프

1. Spring Boot를 초기부터 도입하는 선택

장점:

- `AGENTS.md`의 백엔드 방향과 바로 맞는다.
- API, validation, persistence 테스트까지 한 번에 연결할 수 있다.

단점:

- 현재 빈 저장소에 비해 초기 설정량이 증가한다.

판단:

- 이 프로젝트 목표와 문서 제약을 만족하려면 포함하는 편이 맞다.

2. persistence를 초기 MVP에 포함하는 선택

장점:

- report 저장과 rule version 저장까지 초기에 검증할 수 있다.
- 단순 계산기 수준이 아니라 실제 백엔드 흐름을 만든다.

단점:

- 스키마를 너무 빨리 고정할 위험이 있다.

판단:

- 최소 필드만 저장하는 단순 구조로 시작하는 것이 적절하다.

3. harness를 초기에 최소 포함하는 선택

장점:

- 문서가 요구하는 재현성과 회귀 검증을 빠르게 만족시킨다.
- 평가 로직 변경 시 안전망이 생긴다.

단점:

- 초기 구현 범위가 넓어질 수 있다.

판단:

- 전체 프레임워크를 크게 만들지 말고, fixture 1~2개와 comparator 중심으로 시작한다.

4. risk rule을 풍부한 규칙 객체로 바로 나눌지 여부

장점:

- 규칙 수가 늘 때 확장성이 좋다.

단점:

- 초기에는 과설계가 될 수 있다.

판단:

- 첫 버전은 `RiskScoringEngine` 내부의 명시적 규칙 집합으로 시작하고, 규칙 수가 늘면 분리한다.

5. 도메인 타입을 어디까지 세분화할지 여부

장점:

- 잘못된 값 조합을 컴파일 단계와 생성 시점에서 빨리 막을 수 있다.
- DDD 관점에서 의미가 드러나고 테스트가 읽기 쉬워진다.

단점:

- 초기 파일 수와 매핑 코드가 늘어난다.

판단:

- ID, code, version, product name, capability처럼 의미가 명확한 값은 전용 타입으로 분리한다.
- 단순 내부 메시지 문자열까지 모두 타입으로 쪼개지는 않는다.

## 6. 리스크

1. 입력 계약이 아직 확정되지 않았다.

- `AssessmentRequest` 필드 구조가 실제 제품 요구와 다를 수 있다.

2. persistence 선택이 바뀔 수 있다.

- 로컬 H2, Postgres, 다른 저장 방식 중 무엇을 쓸지 아직 명시되지 않았다.

3. 첫 규칙 세트가 도메인을 과도하게 단순화할 수 있다.

- 문서가 요구하는 "launch decision support" 수준에 비해 초반 룰이 빈약할 수 있다.

4. harness 범위가 다시 밀릴 수 있다.

- API 구현에만 집중하면 문서 핵심 요구를 놓치기 쉽다.

5. 승인 중심 워크플로우가 속도를 늦출 수 있다.

- 하지만 이 프로젝트에서는 속도보다 사용자 통제와 검증 가능성이 우선이다.

6. DDD를 과도하게 적용할 수 있다.

- 초기 범위를 넘어서 aggregate와 객체 수가 불필요하게 늘어날 수 있다.
- 따라서 첫 단계에서는 assessment와 harness 두 영역만 명확히 나누고, 나머지는 단순하게 유지해야 한다.

## 7. planning 단계 산출물 기준

이 계획의 목적은 구현을 시작하기 전에 아래를 고정하는 것이다.

- 첫 vertical slice 범위
- 패키지 구조
- 테스트 우선 구현 순서
- 핵심 API 계약 초안
- persistence 최소 필드
- 도메인 전용 타입 경계
- harness 최소 범위

이 단계에서는 구현하지 않는다. annotation 단계에서 메모와 수정 요청을 반영한 뒤, 승인되면 tasks 단계로 넘어간다.
