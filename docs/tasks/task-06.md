# Task 06

- Goal
  assessment 생성 API와 에러 응답 계약을 integration test 중심으로 구현한다.

- Files
  `src/test/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentApiIntegrationTest.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentRequest.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentResponse.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/api/AssessmentController.kt`
  `src/main/kotlin/me/hanhyur/gatewell/assessment/application/AssessmentService.kt`
  `src/main/kotlin/me/hanhyur/gatewell/common/api/ApiErrorResponse.kt`
  `src/main/kotlin/me/hanhyur/gatewell/common/api/GlobalExceptionHandler.kt`

- Requirements
  `POST /assessments`가 정상 요청과 validation 실패를 처리해야 한다.
  request DTO는 raw input을 받고 application command로 변환해야 한다.
  response는 summary, findings, severity, recommendation, launchDecision, evidence를 포함해야 한다.
  에러는 machine-readable 구조를 가져야 한다.

- Constraints
  controller에 도메인 판단 로직을 넣지 않는다.
  API 계약은 현재 MVP 범위만 다룬다.
  인증, 권한, 조회 API까지 범위를 넓히지 않는다.

- Done Criteria
  integration test가 성공/실패 케이스를 검증한다.
  controller와 application service wiring이 동작한다.
  validation 오류와 일반 오류의 응답 형식이 정리된다.

