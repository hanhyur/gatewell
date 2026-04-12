# Task 04

- Goal
  `LaunchDecisionPolicy`와 report 조합에 필요한 decision 도메인을 TDD로 구현한다.

- Files
  `src/test/kotlin/me/hanhyur/gatewell/assessment/domain/policy/LaunchDecisionPolicyTest.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/Assessment.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/AssessmentId.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/model/AssessmentReport.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/policy/LaunchDecisionPolicy.kt`

- Requirements
  severity와 findings를 바탕으로 launch decision과 recommendation을 산출해야 한다.
  assessment와 report aggregate 초안을 정의해야 한다.
  decision 로직은 테스트로 먼저 고정해야 한다.

- Constraints
  persistence 세부사항을 aggregate에 노출하지 않는다.
  API response shape에 맞춘 필드만 억지로 넣지 않는다.
  규칙별 explanation은 최소 범위만 포함한다.

- Done Criteria
  policy 테스트가 존재한다.
  `AssessmentReport.create(...)` 수준의 조합 API가 존재한다.
  severity별 decision 산출 규칙이 문서화된 테스트로 검증된다.

