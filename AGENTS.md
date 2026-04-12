# AGENTS.md

## 1. Project Overview
This project is an AI-native backend system that evaluates whether an AI-powered product is safe to launch.

The system must:
- identify real launch risks
- prioritize them
- produce actionable recommendations
- support launch decisions (allow / caution / block)

This is NOT a checklist generator.
This is a risk evaluation platform with reproducible and testable decision logic.

---

## 2. User Authority (HIGHEST PRIORITY)

- All decisions are made by the user.
- Do not proceed arbitrarily.
- Always ask before:
    - file creation / modification / deletion
    - command execution
    - scope expansion
    - architectural changes
- Proceed only after explicit approval.

If there is any ambiguity that affects externally visible behavior → ask first.

---

## 3. Core Product Philosophy

### 3.1 Actionable Over Theoretical
Outputs must directly help launch decisions.

### 3.2 Risk Prioritization
Focus on top impactful risks, not exhaustive lists.

### 3.3 Deterministic Where Possible
Same input → same result.

### 3.4 MVP First
Avoid enterprise complexity early.

### 3.5 Trust Through Reproducibility
The system must be verifiable and testable over time.

---

## 4. Project Pipeline (MANDATORY)

Follow this pipeline unless explicitly overridden:

1. research.txt
2. planning.txt
3. annotation.txt
4. Repeat 2–3 until approved
5. tasks.txt
6. implementation.txt
7. review.txt

Rules:
- Do not skip or reorder
- Each `.txt` is a stage template
- Treat them as working artifacts (not production code)

---

## 5. Context

- Backend-focused project
- Primary truth: user instructions
- Secondary: repository code/docs
- `.txt` files are local working notes (not committed)

---

## 6. Delivery Goal

- Deliver requested functionality first
- Show strong backend engineering judgment
- Avoid unnecessary scope expansion

---

## 7. Core Domain

Entities:

- Assessment
- RiskFinding
- Severity
- Recommendation
- LaunchDecision
- Evidence

---

## 8. Risk Categories

- prompt injection
- data leakage
- harmful output
- auth weakness
- abuse/spam
- rate limiting
- cost explosion
- observability gaps
- fallback failures

---

## 9. Architecture Expectations

System must be modular:

- API Layer
- Risk Engine
- Reporting Layer
- Persistence Layer
- Harness Layer

Example structure:

- apps/api
- services/risk-engine
- services/harness
- packages/domain

---

## 10. Harness Engineering (CRITICAL)

### 10.1 Purpose
Ensure:
- reproducibility
- regression detection
- evaluation consistency
- trust in decisions

### 10.2 Capabilities
- scenario-based evaluation
- adversarial testing
- regression suites
- expected vs actual comparison
- rule version tracking

### 10.3 Flow Separation

Product:
user → evaluation → report

Harness:
scenario → evaluation → compare → result

### 10.4 Reproducibility

Track:
- rule version
- scenario version
- input fixture
- timestamp

### 10.5 Trust Layer

Harness is NOT optional.
It guarantees system reliability.

---

## 11. Implementation Rules

- Use Kotlin + Spring Boot (as provided)
- Prefer simple, explicit design
- Keep changes surgical
- Separate domain / controller / persistence

---

## 12. Coding Principles

- simple > complex
- explicit > implicit
- modular > monolithic
- testable > hidden logic

Avoid:
- over-abstraction
- god classes
- unnecessary frameworks

---

## 13. API Design Expectations

- consistent request/response shape
- strict validation
- clear error handling
- machine-readable outputs

---

## 14. Persistence Rules

Store:
- input
- findings
- decision
- timestamps
- rule version

---

## 15. Testing Requirements

Must include:

- unit tests (risk rules)
- decision logic tests
- integration tests (API)
- harness regression tests

Focus especially on:
- scoring logic
- decision outcomes

---

## 16. Quality Bar

- every step must be verifiable
- behavior must be testable
- code must be readable without explanation

---

## 17. Review Heuristics

- ask if ambiguity affects behavior
- choose simplest valid solution
- optimize for reviewability
- separate future improvements from current implementation

---

## 18. Commit Rules

- no single large commit
- split by logical unit
- Angular-style commit messages
- exclude `.txt` files

---

## 19. Documentation Rules

- AGENTS.md / CODEX.md are system-level guides
- `.txt` files are local-only artifacts
- do not commit working notes

---

## 20. Source Priority

1. user instructions
2. repository code/docs

If conflict:
→ follow higher priority and document tradeoff

---

## 21. Execution Plan

1. convert requirements → API + domain tasks
2. implement minimal vertical slice
3. verify high-risk logic
4. prepare reviewable state

---

## 22. Non-Goals

Do NOT:
- introduce unrelated infra
- over-engineer architecture
- optimize prematurely
- add enterprise features early

---

## 23. Final Rule

This system must be trustworthy not only in what it evaluates,
but also in how its evaluation logic is verified over time.