# Mini API Gateway (MAG) — AI Usage Log

This log records every significant use of Generative AI tools across all milestones in accordance with academic integrity guidelines, IEEE 730 SQAP, and SE423/SE431 course requirements.

Each entry must contain:
1. **Member:** Full name & registration number.
2. **Module / Deliverable:** Associated module or documentation section.
3. **Tool used:** Name and model version (e.g., Gemini 3.1 Pro, Claude Sonnet 4.6, GitHub Copilot).
4. **Purpose:** Specific technical or documentation task.
5. **Relevant prompt or activity summary:** Concise synopsis of the prompt or problem posed.
6. **AI-generated material adopted:** Precise components, functions, or text incorporated into the repository.
7. **Modifications made:** Human adaptations, corrections, refactorings, or architectural alignments.
8. **Verification performed:** Independent validation method (unit tests, manual tracing, inspection, Checkstyle).

---

## Milestone 1 Entries

### Entry 001 — Architecture & Project Skeleton Foundation
* **Date:** 12 October 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** Project Multi-Module Architecture & Parent POM
* **Tool used:** Gemini 3.1 Pro
* **Purpose:** Generate standard multi-module Maven configuration enforcing strict separation between `gateway-core` (pure Java) and `gateway-app` (Spring Boot shell).
* **Relevant prompt or activity summary:** "Configure Maven parent POM and sub-module POMs ensuring gateway-core has zero Spring dependencies and declares pinned dependency versions."
* **AI-generated material adopted:** Maven `dependencyManagement` section, compiler plugin configuration, and sub-module declarations.
* **Modifications made:** Added strict JaCoCo 60% coverage threshold, Checkstyle 10-complexity check, and pinned Java 21 LTS properties.
* **Verification performed:** Verified POM structure and syntax; confirmed `gateway-core` cannot resolve Spring classes.

### Entry 002 — Deterministic Clock Abstraction
* **Date:** 12 October 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** M-A / Supporting Components (`Clock` abstraction)
* **Tool used:** Gemini 3.1 Pro
* **Purpose:** Create `FakeClock` test double enabling deterministic testing without `Thread.sleep` (MAG-QR-10, Risk R6).
* **Relevant prompt or activity summary:** "Implement a thread-safe FakeClock using AtomicReference<Instant> with an advance(Duration) method for unit tests."
* **AI-generated material adopted:** `FakeClock.java` structure and `advance()` implementation.
* **Modifications made:** Made class test-scoped under `gateway-core/src/test`, added boundary validation against negative time jumps.
* **Verification performed:** Written unit tests verifying time moves forward only on explicit invocation.

### Entry 003 — Rate Limiter Strategy & Factory Contracts
* **Date:** 12 October 2026
* **Member:** Anamta Akmal (2023113)
* **Module / Deliverable:** M-B / Identity & Traffic Control
* **Tool used:** Claude Sonnet 4.6
* **Purpose:** Generate GoF Strategy pattern interfaces and Factory skeleton for pluggable rate limiting.
* **Relevant prompt or activity summary:** "Design a Strategy pattern interface for rate limiting with TokenBucket, FixedWindow, and SlidingWindowCounter skeletons."
* **AI-generated material adopted:** `RateLimitAlgorithm.java`, `RateLimitDecision.java`, and `RateLimiterFactory.java` interfaces.
* **Modifications made:** Aligned method signature to return immutable `RateLimitDecision` (allowed, remainingTokens, retryAfterSeconds); integrated injected `Clock`.
* **Verification performed:** Inspected against MAG-QR-11 (open/closed principle: adding an algorithm requires zero changes to existing ones).

### Entry 004 — Circuit Breaker State Machine & Demo Dashboard
* **Date:** 12 October 2026
* **Member:** Jagtar Singh (2023266)
* **Module / Deliverable:** M-D / Resilience Engine & Demo Dashboard UI
* **Tool used:** Gemini 3.1 Pro
* **Purpose:** Draft GoF State pattern for Circuit Breaker and vanilla static dashboard layout.
* **Relevant prompt or activity summary:** "Create CircuitBreaker with ClosedState, OpenState, HalfOpenState and a vanilla 3-panel HTML/JS demo dashboard."
* **AI-generated material adopted:** State interface signatures, state transition scaffolding, and HTML/CSS structure for dashboard panels.
* **Modifications made:** Swapped direct `System.currentTimeMillis()` calls with injected `Clock`; styled dashboard with dark-mode glassmorphic theme preserving all element IDs.
* **Verification performed:** Served dashboard locally on port 8080 and validated HTTP rendering and responsive layout across devices.

---

## Milestone 2 Entries

### Entry 005 — P0-1 Interface Freeze
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P0-1 interface lock, `ConfigSnapshot`, `HttpUpstreamClient`, P0-1 contract tests, `docs/m2-interface-lock.md`
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Complete the missing P0-1 configuration snapshot record and HTTP 501 skeleton while preserving existing contract interfaces.
* **Relevant prompt or activity summary:** Implement P0-1 only: add focused red tests, convert `ConfigSnapshot` to a defensive immutable record, make `HttpUpstreamClient.execute()` return 501, and document SE423/SE431 evidence.
* **AI-generated material adopted:** `ConfigSnapshotTest`, `HttpUpstreamClientTest`, the record implementation, the 501 stub implementation, and `docs/m2-interface-lock.md`.
* **Modifications made:** Kept existing P0-1 interfaces unchanged; added compatibility `getVersion()`/`getRoutes()` accessors to avoid breaking the Spring shell; added a null-request guard.
* **Verification performed:** 22 November 2026 `mvn clean verify`: gateway-core 55 tests, 0 failures, 0 errors, 3 skipped; gateway-app 13 tests, 0 failures, 0 errors; BUILD SUCCESS.

### Entry 006 — P1-1 Request Pipeline and Authentication
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P1-1 request-ID, JWT auth, forwarding pipeline, exception mapping, and `docs/m2-p1-1-pipeline.md`
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Implement the P1-1 behaviors from the M2 execution plan without changing Maven or Checkstyle quality gates.
* **Relevant prompt or activity summary:** Add test-first behavior for request IDs, Bearer validation, forwarding, exception mapping, short-circuiting, and malformed route configuration; wire the pipeline into the servlet.
* **AI-generated material adopted:** Focused core and app tests; `RequestIdFilter`, `AuthFilter`, `ForwardFilter`, `JjwtTokenVerifier`, centralized exception response mapping, servlet pipeline wiring, and P1-1 evidence documentation. Added separate JJWT token-class tests, per-subclass mapper status tests, separate request-ID behavior tests, and a spy-based auth short-circuit assertion.
* **Modifications made:** Kept `gateway-core` independent of JWT implementation classes; pinned verification to signed HS256 tokens; configured the existing app HTTP client to remain responsible for connect/read timeouts. Changed only the app JWT placeholder value to satisfy JJWT's minimum HMAC key length.
* **Verification performed:** 22 November 2026 `mvn clean verify`: gateway-core 55 tests, 0 failures, 0 errors, 3 skipped; gateway-app 13 tests, 0 failures, 0 errors; all Maven and JaCoCo gates passed.

### Entry 007 — P2-1 SOLID Contracts
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P2-1 OCP proof, upstream and rate-limit LSP tests, SOLID notes
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Add test-first extension and substitutability contracts without modifying AA- or JS-owned implementations.
* **Relevant prompt or activity summary:** Add ForwardFilter-facing UpstreamClient contracts, an OCP registry proof, and a shared rate-limit contract for strategies that are currently stubs.
* **AI-generated material adopted:** New core/app contract tests and [P2-1 SOLID review](../p2-1-solid-review.md).
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm; Maven was not run per instruction)`

### Entry 008 — P3-3 Verification and Validation Plan
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P3-3, `docs/vv-plan/m2-vv-plan.md`
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Document IEEE 1012-style V&V activities for M-A through M-E.
* **Relevant prompt or activity summary:** Map MAG-FR-01 through MAG-QR-13 to M1 RTM IDs, methods, owners, milestones, and evidence placeholders.
* **AI-generated material adopted:** Activity matrix, scope, V/Val distinction, and review guidance.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm; documentation not validated by Maven)`

### Entry 009 — P0-2 Kickoff Draft
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P0-2, `docs/m2-kickoff.md`
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Prepare the standing-agreements draft without claiming repository or GitHub settings.
* **Relevant prompt or activity summary:** Record the planned commit prefixes, merge/CI policy, and external inspection dependency; leave contacts and SCM details for AN.
* **AI-generated material adopted:** Text-only kickoff table and course mapping.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm; documentation not validated by Maven)`

### Entry 010 — P4-1 Architecture Documentation
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P4-1 README, ADRs 001–003, and patterns guide
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Document architecture, pattern rationale, alternatives, and verification paths using source-supported status.
* **Relevant prompt or activity summary:** Update README and write three decision records plus problem/alternative/verification notes for six patterns.
* **AI-generated material adopted:** `README.md`, `docs/architecture-decisions/adr-001-fail-open-vs-closed.md`, `adr-002-single-flight-cache.md`, `adr-003-snapshot-swap-vs-locking.md`, and `docs/patterns.md`.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm; documentation not validated by Maven)`

### Entry 011 — P2-2 Coverage Baseline Template
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P2-2, `docs/m2-coverage-baseline.md`
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Provide an auditable template for actual coverage and quality-gate evidence.
* **Relevant prompt or activity summary:** Leave all run-dependent values pending and do not treat the pre-P1-1 datapoint as the M2 baseline.
* **AI-generated material adopted:** Baseline fields, evidence guidance, and rubric mapping.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm; Maven was not run per instruction)`

### Entry 012 — P3-7 RTM and Risk Update
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** P3-7, `docs/rtm/rtm-v2.md` and `docs/risk-register.md`
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Update traceability and risk/CAPA structure using only available source and plan evidence.
* **Relevant prompt or activity summary:** Carry forward M1 requirement IDs, mark source status, and leave unprovided test IDs, inspection findings, ratings, and coverage as explicit TODOs.
* **AI-generated material adopted:** RTM v2, risk register, and CAPA table skeleton.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm; documentation not validated by Maven)`

### Entry 013 — AN Milestone 2 LaTeX Documentation Update
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** AN-owned M2 sections in the SE423 and SE431 LaTeX documents
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Update the current M2 SE423 and SE431 documents only for AN's pipeline, patterns, defensive programming, SOLID, TDD/coverage, repository documentation, V&V, RTM/risk/CAPA, coverage baseline and inspection author-role evidence.
* **Relevant prompt or activity summary:** Reuse repository Markdown and source evidence, preserve M2 LaTeX style and labels, keep teammate sections as TODO placeholders, and avoid GitHub/PR/branch claims and unverified percentages or results.
* **AI-generated material adopted:** AN-owned LaTeX section revisions and an explicit verified 22 November 2026 build-baseline summary.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm)`

### Entry 014 — AN Milestone 2 Test and Documentation Completion
* **Date:** 22 November 2026
* **Member:** Ahmad Nawaz (2023085)
* **Module / Deliverable:** AN-owned P1-1 test-coverage review, P2-2 coverage values, risk reassessment, and M2 LaTeX consistency
* **Tool used:** GitHub Copilot (Copilot SDK in VS Code); model version `gpt-5.6-luna`
* **Purpose:** Complete the remaining AN items already supported by existing tests and verified JaCoCo evidence without changing AA or JS files.
* **Relevant prompt or activity summary:** Confirm P1-1 auth, mapper, request-ID and short-circuit test coverage; fill gateway-core JaCoCo line and branch percentages and the VS Code model name; reassess AN-affected risks; align M2 coverage and test documentation.
* **AI-generated material adopted:** Coverage, model metadata and AN risk/documentation updates.
* **Modifications made:** `TODO(AN: confirm)`
* **Verification performed:** `TODO(AN: confirm)`
