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
