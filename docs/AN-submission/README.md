# Mini API Gateway (MAG)

A lightweight, configuration-driven reverse proxy written in **Java with Spring Boot**. MAG sits between clients and upstream HTTP services and applies request identification, JWT validation, planned rate limiting, GET caching, and circuit breaking through a filter chain we implement ourselves.

> **SE423: Software Construction and Development** | GIK Institute of Engineering Sciences and Technology
> Instructor: Dr. Mian Muaz Razaq



---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Modules and Ownership](#modules-and-ownership)
- [Design Patterns](#design-patterns)
- [SOLID in Practice](#solid-in-practice)
- [Repository Structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Build, Test and Run](#build-test-and-run)
- [Configuration Example](#configuration-example)
- [Demo Dashboard](#demo-dashboard)
- [Concurrency and Thread Safety](#concurrency-and-thread-safety)
- [Error Handling](#error-handling)
- [Database Schema](#database-schema)
- [Testing Approach](#testing-approach)
- [Branching and Commit Conventions](#branching-and-commit-conventions)
- [Documentation and AI Usage Log](#documentation-and-ai-usage-log)
- [Scope](#scope)
- [Team](#team)

---

## Features

- **Request identification:** `X-Request-Id` header and structured logging
- **Authentication:** stateless JWT validation
- **Rate limiting:** planned token bucket, fixed-window, and sliding-window strategies; current status is listed below
- **Caching:** safe GET response caching with TTL, bounded LRU eviction and single-flight loading on a miss
- **Resilience:** circuit breaker (closed, open, half-open) and upstream timeouts
- **Routing and forwarding** to upstream services
- **Hot-reloadable configuration:** PostgreSQL-backed, managed through an Admin REST API
- **Demo dashboard:** a single static page for metrics, rules and a request tester

MAG is **generic**: routes, limits and cache rules are data, not code, so it can protect any HTTP service without modification.

## Architecture

MAG is split into a **plain-Java core** (no Spring annotations) and a thin **Spring Boot shell**.

- `gateway-core` holds all business logic. A multi-module Maven build makes the compiler enforce that the core cannot import Spring.
- `gateway-app` provides the servlet entry point, dependency-injection wiring, JPA repositories, metrics and the static dashboard.
- The entry point hands each request to **our** filter chain, not Spring's.

### Request flow

```
Client
  -> Servlet Entry Point (Spring shell)
  -> RequestIdFilter
  -> AuthFilter (JWT on protected routes)
  -> RateLimitFilter (implementation pending)
  -> ForwardFilter
  -> CachingUpstreamClient       (Proxy: cache + single-flight)
  -> ResilientUpstreamClient     (Circuit breaker)
  -> SimpleHttpUpstreamClient    (connect/read timeouts)
  -> Upstream services
```

The cache is a proxy behind the `UpstreamClient` interface and the circuit breaker wraps the HTTP client. A cache hit therefore never reaches the breaker and cannot distort failure statistics.

Supporting components: `ConfigSnapshot` (held in an `AtomicReference`), Config Manager + Admin API (PostgreSQL), a central exception mapper, and an injected `Clock`.

### Dependency rules

1. The shell depends on the core, never the reverse.
2. Modules meet only at interfaces.
3. No cyclic dependencies.
4. Time comes only from the injected `Clock`.

## Modules and Ownership

| ID  | Module                     | Scope                                                                                                           | Owner         | M2 source status |
|-----|----------------------------|-----------------------------------------------------------------------------------------------------------------|---------------|---|
| M-A | Proxy Pipeline             | Route matching, `GatewayFilter` chain, forwarding, header handling, timeouts, central exception-to-HTTP mapping | Ahmad Nawaz | Pipeline source exists; see [P1-1 evidence](docs/m2-p1-1-pipeline.md); test execution unverified. |
| M-B | Identity & Traffic Control | JWT validation, pluggable client-key extraction, thread-safe rate limiting (three algorithms)                  | Anamta Akmal | Auth source exists; rate-limit filter, strategies, and key extractors remain stubs. |
| M-C | Fast-Path Cache            | Safe GET caching with TTL, bounded LRU eviction, single-flight loading                                          | Jagtar Singh | Cache and focused test source exist; execution evidence pending. |
| M-D | Resilience Engine          | Circuit-breaker state machine with failure tracking and probing                                                 | Jagtar Singh | State implementation and focused test source exist; execution evidence pending. |
| M-E | Configuration & Admin      | PostgreSQL routes and rules, validation, immutable snapshot hot reload, last-known-good fallback, Admin REST API | Anamta Akmal | Admin and snapshot source exist; full validation evidence pending. |

**Supporting components:** `Clock` abstraction, metrics and logging, dummy upstreams, JWT minter, load script (Ahmad Nawaz); demo dashboard (Jagtar Singh). CI workflow status: `TODO(AN: fill; confirm)`.

**Ownership rules**

- Each member writes, tests and defends their own modules.
- Every pull request is reviewed by another member; concurrency code gets a second human review.

## Design Patterns

| Pattern                 | Type       | Where                                         |
|-------------------------|------------|-----------------------------------------------|
| Chain of Responsibility | Behavioral | `GatewayFilter` pipeline                      |
| Strategy                | Behavioral | `RateLimitAlgorithm`, `KeyExtractor`          |
| State                   | Behavioral | `CircuitBreaker` (`CircuitState`)             |
| Factory                 | Creational | `RateLimiterFactory`                          |
| Builder                 | Creational | `PipelineBuilder`                             |
| Proxy                   | Structural | `CachingUpstreamClient`                       |

Pattern rationale, alternatives, and verification paths are documented in
[patterns.md](docs/patterns.md); listed tests are not represented as executed.

## SOLID in Practice

| Principle | Decision |
|-----------|----------|
| **S**ingle Responsibility | One concern per class: each filter does one job; `ConfigValidator`, `ConfigRepository` and `SnapshotPublisher` are separate; error mapping lives only in the central mapper. |
| **O**pen/Closed | The factory registry provides the extension point; the proof test is in [RateLimiterFactoryOcpTest.java](gateway-core/src/test/java/com/mag/core/ratelimit/RateLimiterFactoryOcpTest.java). |
| **L**iskov Substitution | Contract-test source covers the upstream clients and rate-limit strategies; the latter implementations remain stubs pending AA. |
| **I**nterface Segregation | Filters see only read-only `ConfigProvider`; only the admin module sees `ConfigPublisher`; `Clock` exposes only `now()`. |
| **D**ependency Inversion | The core depends on abstractions (`Clock`, `UpstreamClient`, `ConfigProvider`, `ConfigRepository`); wiring is by constructor injection in the shell only. |

## Repository Structure

```
mini-api-gateway/
|-- gateway-core/        # plain Java: filters, algorithms, cache, breaker
|-- gateway-app/         # Spring Boot shell: wiring, JPA, admin API,
|                        #   static demo dashboard (resources/static)
|-- dummy-upstreams/     # test fixtures: /fast /slow /flaky /cacheable
|-- tools/               # JWT minter, load scripts
|-- docs/                # design docs, ADRs, AI usage log
|-- README.md
```

## Prerequisites

- JDK 21 (as specified in the root `pom.xml`)
- Apache Maven
- Docker (for PostgreSQL and Testcontainers-based tests)

## Build, Test and Run

> Commands below are the standard Maven flow; adjust module names and ports to match your final `pom.xml` and configuration.

```bash
# Clone
git clone https://github.com/<org>/mini-api-gateway.git
cd mini-api-gateway

# Start PostgreSQL (example)
docker run -d --name mag-db \
  -e POSTGRES_DB=mag -e POSTGRES_USER=mag -e POSTGRES_PASSWORD=change-me \
  -p 5432:5432 postgres:16

# Build and run all tests and quality gates
mvn clean verify

# Run the gateway
mvn -pl gateway-app spring-boot:run

# Run the dummy upstream services
mvn -pl dummy-upstreams spring-boot:run
```

Flyway migrations run automatically at startup and create the schema.

## Configuration Example

Routes and rate-limit rules are stored in PostgreSQL and managed through the Admin REST API. The shape below follows the schema (`routes` and `rate_limit_rules`):

```json
{
  "pathPattern": "/api/items/**",
  "upstreamUrls": ["http://localhost:9001"],
  "methods": ["GET", "POST"],
  "authRequired": true,
  "cacheEnabled": true,
  "cacheTtlSeconds": 30,
  "priority": 10,
  "rateLimit": {
    "algorithm": "TOKEN_BUCKET",
    "keyStrategy": "JWT_SUBJECT",
    "limit": 100,
    "windowSeconds": 60,
    "burst": 20
  }
}
```

Every change is validated, then published as an immutable `ConfigSnapshot`. If a new configuration is invalid, the last known good version stays active.

## Demo Dashboard

A single static HTML/JavaScript page served by the gateway with three panels:

1. **Live metrics:** allowed/blocked counts, cache hit ratio, circuit state
2. **Routes and rules:** view routes and edit a limit through the Admin API
3. **Request tester:** send a request with a chosen token and see status and headers

The dashboard contains no logic of its own; it only calls the Admin API and metrics endpoint. The admin token is typed into the page and is never stored in code.

## Concurrency and Thread Safety

| Component        | Hazard                                      | Strategy                                                              |
|------------------|---------------------------------------------|-----------------------------------------------------------------------|
| Rate-limit state | Check-then-act race                         | Per-key atomic update with `ConcurrentHashMap.compute` on immutable state |
| Configuration    | Reader/writer conflict on reload            | Immutable `ConfigSnapshot` in an `AtomicReference` (copy-on-write)    |
| Cache            | Concurrent eviction; stampede on expiry     | Bounded LRU under a lock; per-key single-flight with `CompletableFuture` |
| Circuit breaker  | Concurrent success/failure reports          | Atomic counters; state change by compare-and-set                      |
| Background work  | Idle-key cleanup, config poller             | `ScheduledExecutorService` with defined shutdown                      |

## Error Handling

A `GatewayException` base class, mapped to HTTP responses by **one central mapper** (no generic `catch (Exception)`):

| Exception                       | HTTP |
|---------------------------------|------|
| `AuthenticationException`       | 401  |
| `AuthorizationException`        | 403  |
| `RateLimitExceededException`    | 429  |
| `UpstreamUnavailableException`  | 502  |
| `CircuitOpenException`          | 503  |
| `UpstreamTimeoutException`      | 504  |
| `InvalidConfigurationException` | n/a (rejected before becoming a snapshot) |

Every module validates its inputs at the boundary (null, empty, negative limits, malformed JWT, bad route patterns). The fail-open vs. fail-closed policy is recorded in an Architecture Decision Record under `docs/`.

## Database Schema

PostgreSQL with versioned Flyway migrations.

| Table               | Key columns                                                                                                           | Purpose                       |
|---------------------|-----------------------------------------------------------------------------------------------------------------------|-------------------------------|
| `routes`            | `id`, `path_pattern`, `upstream_urls`, `methods`, `auth_required`, `cache_enabled`, `cache_ttl_seconds`, `priority`  | Routing rules by priority     |
| `rate_limit_rules`  | `id`, `route_id` (FK), `algorithm`, `key_strategy`, `limit`, `window_seconds`, `burst`                               | Per-route limiter settings    |
| `config_versions`   | `version`, `created_at`, `created_by`, `snapshot_json`, `is_active`                                                   | Versioning, last-known-good   |

## Testing Approach

- **Tools:** JUnit 5, Mockito, JaCoCo
- **Core:** plain unit tests; Spring context tests only for a few integration scenarios
- **Deterministic time:** an injected fake `Clock`, so there are no sleeps in tests
- **TDD:** red-green-refactor is the planned workflow; commit-trail evidence is not asserted here
- **Coverage:** JaCoCo is configured in the Maven build; the M2 baseline and CI verification are pending. See the [baseline template](docs/m2-coverage-baseline.md).

## Branching and Commit Conventions

The intended lightweight **GitHub Flow** policy is recorded below. Current
Git/GitHub settings have not been verified in this workspace; see the
[kickoff draft](docs/m2-kickoff.md).

| Element           | Rule                                                                                               |
|-------------------|----------------------------------------------------------------------------------------------------|
| `main`            | Protection/green-CI rule: `TODO(AN: fill; confirm)`                                                |
| Feature branches  | Naming and protection: `TODO(AN: fill; confirm)`                                                    |
| Pull requests     | Review requirements and current settings: `TODO(AN: fill; confirm)`                                  |
| Merge method      | Merge commit or rebase-and-merge; squash setting: `TODO(AN: fill; confirm)`                        |
| Commit prefixes   | `test:` (red), `feat:` (green), `refactor:`, `fix:`, `docs:`, `chore:`                             |
| Baselines         | Tag policy and existing tags: `TODO(AN: fill; confirm)`                                             |
| Quality gate      | Required CI checks and current status: `TODO(AN: fill; confirm)`                                    |

The workflow file and GitHub Actions status have not been inspected or verified.

## Documentation and AI Usage Log

`docs/` contains the [architecture decisions](docs/architecture-decisions/),
[pattern rationale](docs/patterns.md), [V&V plan](docs/vv-plan/m2-vv-plan.md),
[RTM](docs/rtm/rtm-v2.md), and [risk register](docs/risk-register.md).

The active AI log currently resides at
[docs/Milestone 1/ai-usage-log.md](<docs/Milestone 1/ai-usage-log.md>).
The M2 TeX log file is a separate template and has not been mirrored.

## Scope

**In scope**

- The five modules above
- Test fixtures: dummy upstreams (`/fast`, `/slow`, `/flaky`, `/cacheable`), a JWT-minting utility and a load script
- The three-panel demo dashboard

**Out of scope**

- Reading or modifying request/response bodies (only URLs, headers and methods)
- User registration (JWTs are minted elsewhere and only verified here)
- Distributed mode (documented, not implemented)
- Dashboard features beyond the three panels

## Team

| Name         | Roll No. | Modules                         |
|--------------|----------|---------------------------------|
| Ahmad Nawaz  | 2023085  | M-A, supporting components, CI  |
| Anamta Akmal | 2023113  | M-B, M-E                        |
| Jagtar Singh | 2023266  | M-C, M-D, dashboard             |

---

*SE423 / SE431 Joint Semester Project, Milestone 2 working documentation.*

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Architecture, modules, patterns, build/run guidance, and source-status caveats are documented. |
| SE431 | V&V, RTM, risk, baseline, and SCM evidence paths are linked without claiming unverified results. |
