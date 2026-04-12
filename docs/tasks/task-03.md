# Task 03

- Goal
  리스크 평가 규칙을 담당하는 `RiskScoringEngine`을 TDD로 구현한다.

- Files
  `src/test/kotlin/me/hanhyur/gatewell/assessment/domain/policy/RiskScoringEngineTest.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/application/AssessmentCommand.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/policy/RiskScoringEngine.kt`

- Requirements
  평가 로직은 deterministic해야 한다.
  최소 1~2개의 명시적 규칙을 테스트로 고정해야 한다.
  입력은 domain-aware command를 사용해야 한다.
  출력은 findings와 severity를 포함해야 한다.

- Constraints
  규칙 객체 분해는 하지 않는다.
  외부 API 호출이나 랜덤 요소를 넣지 않는다.
  recommendation과 launch decision 계산은 이 작업 범위에 포함하지 않는다.

- Done Criteria
  실패하는 규칙 테스트가 먼저 작성돼 있다.
  `RiskScoringEngine`이 테스트를 통과한다.
  command와 scoring result가 이후 decision policy에서 재사용 가능하다.

