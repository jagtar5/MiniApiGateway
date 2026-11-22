# ADR-002: Per-Key Single-Flight Cache

| Field | Value |
|---|---|
| Status | Proposed for M2 review |
| Scope | Concurrent cache misses in the upstream cache decorator |

## Context

Concurrent requests for the same uncached response can issue duplicate
upstream calls. Serializing all cache traffic would also block unrelated keys.

## Decision

Use per-key in-flight work so requests for the same key share one load while
different keys can proceed independently. The current implementation is in
[CachingUpstreamClient.java](../../gateway-core/src/main/java/com/mag/core/upstream/impl/CachingUpstreamClient.java).

## Rejected alternatives

- Allow every concurrent miss to call upstream, multiplying work during bursts.
- Use one global lock, serializing unrelated cache keys.

## Verification

Check that same-key concurrent misses share the load, distinct keys can load
independently, and exceptional loads do not leave a permanent in-flight entry.
Test source: [CachingUpstreamClientTest.java](../../gateway-core/src/test/java/com/mag/core/upstream/impl/CachingUpstreamClientTest.java).
Run/output evidence: `TODO(JS: paste actual command output)`.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Explains the cache concurrency problem, per-key design, and alternatives. |
| SE431 | Links implementation and focused verification source without claiming execution. |
