# Task 05

- Goal
  domain port와 persistence adapter를 추가해 report 저장 경로를 만든다.

- Files
  `src/main/kotlin/me/hanhyur/gatewell/assessment/domain/port/AssessmentReportStore.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/infrastructure/persistence/AssessmentReportEntity.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/infrastructure/persistence/JpaAssessmentReportRepository.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/infrastructure/persistence/AssessmentReportStoreImpl.kt`

- Requirements
  domain은 port에만 의존해야 한다.
  저장 시 input, findings, decision, timestamp, rule version이 보존돼야 한다.
  persistence adapter는 domain report와 entity 간 매핑을 담당해야 한다.

- Constraints
  조회 API까지 확장하지 않는다.
  DB 스키마를 과도하게 정규화하지 않는다.
  domain model에 JPA annotation을 넣지 않는다.

- Done Criteria
  저장 포트 인터페이스와 구현체가 분리돼 있다.
  report 저장에 필요한 최소 entity가 정의돼 있다.
  이후 application service가 port를 통해 저장할 수 있다.

