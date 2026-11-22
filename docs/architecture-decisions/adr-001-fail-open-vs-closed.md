# ADR-001: Fail-Open vs. Fail-Closed

| Field | Value |
|---|---|
| Status | Proposed for M2 review |
| Scope | Authentication, rate-limit enforcement, and optional cache behavior |

## Context

An unavailable dependency or internal error can either permit a request to
continue (fail-open) or reject it (fail-closed). The consequences differ for
security enforcement and an optimization such as caching.

## Decision

Fail closed when authentication or rate-limit enforcement cannot establish that
a request is permitted. An unavailable or invalid auth/rate-limit decision must
not become an allow decision. Treat cache lookup as an optimization: a cache
miss delegates to the upstream client; cache behavior must not bypass auth or
alter the upstream response contract. The rate-limit filter is not yet
implemented, so its operational failure behavior remains to be verified.

## Rejected alternative

Failing open for all components improves availability but can silently disable
security and request protection when those controls fail.

## Verification

Verify invalid/missing credentials are rejected and do not reach the upstream;
verify cache misses delegate normally; after AA implements the limiter, verify
limiter errors do not admit requests. Test IDs and execution evidence:
`TODO(AN/AA: provide actual test references and output)`.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Records failure semantics and the rejected availability-first alternative. |
| SE431 | Defines concrete verification obligations and marks the unimplemented limiter behavior. |
