# Task 07

- Goal
  harness 비교 로직과 시나리오 타입을 구현해 회귀 검증 구조를 만든다.

- Files
  `src/test/kotlin/me/hanhyur/gatewell/harness/domain/HarnessComparatorTest.kt`
  `src/main/kotlin/me/hanhyur/gatewell/harness/domain/ScenarioId.kt`
  `src/main/kotlin/me/hanhyur/gatewell/harness/domain/HarnessResult.kt`
  `src/main/kotlin/me/hanhyur/gatewell/harness/domain/HarnessComparator.kt`

- Requirements
  comparator는 decision, severity, categories, key findings를 비교해야 한다.
  결과는 `scenarioId`, `expectedDecision`, `actualDecision`, `passed`, `mismatches`를 포함해야 한다.
  하네스 타입도 도메인 의미가 있는 값으로 표현해야 한다.

- Constraints
  fixture 로딩까지 한 작업에 다 넣지 않는다.
  단순 문자열 전체 비교에 의존하지 않는다.
  제품 API 코드와 강하게 결합하지 않는다.

- Done Criteria
  comparator 테스트가 mismatch 판별 기준을 고정한다.
  harness 결과 모델이 정의돼 있다.
  comparator가 이후 fixture 기반 테스트에서 재사용 가능하다.

