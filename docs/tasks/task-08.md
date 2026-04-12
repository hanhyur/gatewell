# Task 08

- Goal
  fixture 기반 harness 테스트를 추가해 평가 로직의 회귀 검증을 완성한다.

- Files
  `src/test/resources/harness/prompt-injection-basic.json`
  `src/test/resources/harness/missing-rate-limit.json`
  `src/test/kotlin/me/hanhyur/gatewell/harness/AssessmentHarnessTest.kt`

- Requirements
  최소 2개의 명시적 시나리오 fixture가 있어야 한다.
  fixture는 rule version, scenario version, input fixture, expected outcome을 담아야 한다.
  harness test는 동일 domain/application 흐름을 사용해 actual 결과를 만든 뒤 comparator로 비교해야 한다.

- Constraints
  테스트를 위해 별도 평가 로직을 복제하지 않는다.
  fixture 포맷을 지나치게 복잡하게 만들지 않는다.
  시나리오 수를 늘리는 것보다 재현 가능성을 우선한다.

- Done Criteria
  harness regression 테스트가 존재한다.
  fixture 2개가 버전 정보와 기대 결과를 포함한다.
  로직 변경 시 regression mismatch를 드러낼 수 있다.
