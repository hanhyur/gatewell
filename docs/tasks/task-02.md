# Task 02

- Goal
  assessment 도메인의 value object와 기본 모델을 TDD로 정의한다.

- Files
  `src/test/kotlin/me/hanhyur/gatewell/assessment/domain/model/SeverityTest.kt`
  `src/mai[document_pdf.pdf](../../../../Downloads/document_pdf.pdf)n/kotlin/me/hanhyur/gatewell/assessment/domain/model/ProductName.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Evidence.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/FindingCode.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/RuleVersion.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Capability.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/RiskCategory.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Severity.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Finding.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/ScoringResult.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/LaunchDecision.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Recommendation.kt`

- Requirements
  primitive string/list 사용을 줄이고 의미 있는 타입으로 모델을 구성해야 한다.
  `Severity` 계산 규칙은 테스트로 먼저 정의해야 한다.
  값 객체는 생성 시점에 기본 validation을 수행해야 한다.
  이후 정책 계층이 바로 사용할 수 있는 최소 모델만 만든다.

- Constraints
  aggregate root 구현까지 한 번에 밀어넣지 않는다.
  persistence annotation을 도메인 모델에 넣지 않는다.
  UI나 API DTO concerns를 섞지 않는다.

- Done Criteria
  주요 value object가 생성돼 있다.
  `Severity`와 `Finding` 중심의 기본 도메인 모델이 존재한다.
  모델 생성 규칙과 severity 집계 규칙이 테스트로 검증된다.

