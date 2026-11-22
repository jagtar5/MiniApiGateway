# Milestone 2 Interface Lock

**Owner:** Ahmad Nawaz (2023085)  
**Task:** P0-1  
**Branch:** `feature/an-interface-lock` (created and managed by AN)

## Purpose

P0-1 freezes the core contracts used by M-A, M-B, M-C, M-D and M-E. The
existing pipeline, model, upstream and configuration interfaces were retained;
only the incomplete immutable configuration snapshot and HTTP upstream skeleton
required implementation changes.

## Locked Contracts

The following contracts remain in `gateway-core`:

- `GatewayFilter`
- `FilterChain`
- `GatewayContext`
- `GatewayRequest`
- `GatewayResponse`
- `UpstreamClient`
- `UpstreamResponse`
- `ConfigProvider`
- `ConfigSnapshot`

## Implemented P0-1 Changes

- `ConfigSnapshot` is now a Java record with a defensive copy of `routes`.
  Existing `getVersion()` and `getRoutes()` compatibility accessors remain for
  the Spring shell. This preserves immutable, copy-on-write configuration
  publication (DIP/SRP).
- `HttpUpstreamClient.execute()` now returns an immutable `501 Not Implemented`
  `UpstreamResponse` instead of throwing `UnsupportedOperationException`.
  A null request is rejected at the boundary.

The 501 behavior remains a placeholder in the pure-Java core. P1-1 routes
through the Spring-shell `SimpleHttpUpstreamClient`, which applies configured
connect and read timeouts. See [P1-1 pipeline evidence](./m2-p1-1-pipeline.md).

## Verification

P0-1 test files:

- `ConfigSnapshotTest`: verifies record shape, route-copy behavior and
  unmodifiable route access.
- `HttpUpstreamClientTest`: verifies a 501 status and `Not Implemented` body.

Execution evidence:

```text
TODO(AN: paste terminal output for the focused and gateway-core test commands)
```

No test result or tool-version claim is recorded here until confirmed from
terminal output.

## SE423 Evidence

- **Working core architecture:** P0-1 contracts are defined in the pure-Java core; compilation evidence is `TODO(AN: paste terminal output)`.
- **Design patterns:** interfaces support Proxy/Decorator and
  Chain of Responsibility; `ConfigSnapshot` supports the snapshot pattern.
- **Defensive programming:** null configuration routes and null HTTP requests
  are rejected; route collections are copied and remain unmodifiable.
- **SOLID:** interface segregation keeps configuration writes separate from
  `ConfigProvider`; interface abstraction avoids Spring coupling in core.
- **Repository discipline:** no Git/GitHub action was performed.

## SE431 Evidence

- **Requirements:** supports `MAG-FR-04` by keeping upstream contracts extensible
  for central exception mapping, `MAG-QR-04` through immutable config snapshots,
  and `MAG-QR-11` through interface-based implementation substitution.
- **V&V:** focused P0-1 test source exists; execution evidence is
  `TODO(AN: paste terminal output)`. Remaining teammate implementations stay
  under `TODO(AA: ...)` or `TODO(JS: ...)`.
- **Traceability:** P0-1 is enabling evidence, not final requirement acceptance.

## Rubric Mapping

| Course criterion | Evidence |
|---|---|
| SE423: Working core architecture | Locked interfaces and 501 core stub |
| SE423: Design patterns implemented correctly | Interface/proxy/snapshot contract comments and structure |
| SE423: Defensive programming and exception handling | Null guards and immutable snapshot routes |
| SE423: Code quality and SOLID adherence | ISP/DIP/SRP comments and Java record design |
| SE423: TDD and automated-test baseline | `TODO(AN: paste terminal output for red and green P0-1 steps)` |
| SE423: Repository discipline and traceability | AN-owned P0-1 file/change list; no Git actions |
| SE431: RTM, risk and CAPA update | Traceability links to MAG-FR-04, MAG-QR-04, MAG-QR-11; no fabricated results |
