# CODEX.md

## 1. Purpose

This document defines how coding agents should behave.

Goal:
Build a maintainable, testable, and trustworthy AI risk evaluation system.

---

## 2. Core Priorities

1. domain clarity
2. correctness
3. simplicity
4. maintainability
5. testability

---

## 3. Implementation Style

- prefer simple solutions
- avoid overengineering
- write explicit logic
- keep modules small

---

## 4. Core Flow

1. user submits assessment
2. validate input
3. risk engine evaluates
4. generate findings
5. compute severity
6. produce decision
7. store + return report

---

## 5. Domain Separation

Do NOT mix:
- controllers
- domain logic
- persistence

---

## 6. Naming Rules

Use clear names:
- RiskScoringEngine
- LaunchDecisionPolicy
- AssessmentService

Avoid vague:
- Manager
- Util

---

## 7. Risk Engine Rules

Must be:
- deterministic
- testable
- explainable

Prefer:
- rule objects
- category-based scoring

---

## 8. API Rules

Responses must include:
- summary
- findings
- severity
- recommendation
- launchDecision
- evidence

---

## 9. Persistence Rules

Store:
- input
- findings
- decision
- timestamps
- rule version

---

## 10. Logging

- structured logs
- no secrets
- include request context

---

## 11. Error Handling

- validation errors
- domain errors
- infra errors

No silent failures.

---

## 12. Testing Rules

Must include:
- unit tests (scoring)
- decision tests
- integration tests
- harness tests

---

## 13. Harness Engineering (MANDATORY)

### 13.1 Core Principle
Evaluation logic must be reproducible and regression-testable.

### 13.2 Harness Structure

Separate modules:
- scenario definitions
- fixture loader
- harness runner
- comparator
- regression reporter

---

## 14. Scenario Design

Examples:
- prompt_injection_basic
- data_leakage_case
- cost_explosion_case
- missing_rate_limit

Must be explicit and named.

---

## 15. Comparator Rules

Compare:
- decision
- severity
- categories
- key findings

Avoid only text comparison.

---

## 16. Regression Rules

When logic changes:
- detect changed outcomes
- verify expected behavior
- update fixtures if needed

---

## 17. Harness Output

Must include:
- scenarioId
- expectedDecision
- actualDecision
- passed
- mismatches

---

## 18. MVP Requirements

Minimum:
- assessment submission
- risk scoring
- decision output
- report
- basic harness scenarios

---

## 19. Forbidden Patterns

Do NOT:
- over-engineer
- build microservices early
- mix domain logic into controllers
- create unused abstractions

---

## 20. Final Rule

A change to evaluation logic is incomplete unless
its reproducibility and regression impact can be verified.