# P1-1 — Request Pipeline and Authentication

**Date:** 22 November 2026  
**Scope:** Request IDs, JWT authentication, forwarding, exception mapping, and
filter-chain wiring.

## Implementation

- `RequestIdFilter` creates a UUID per routed request. `ForwardFilter` replaces
  any client-provided request ID before forwarding and includes the generated ID
  in the gateway response.
- `AuthFilter` requires one well-formed Bearer header, delegates token
  verification, stores verified claims in the context, and leaves invalid
  requests outside the forward path.
- `JjwtTokenVerifier` verifies signed HS256 tokens and rejects missing,
  malformed, expired, incorrectly signed, unsecured, and other-algorithm
  tokens. The signing key comes from `MAG_JWT_SECRET`; the development
  placeholder is long enough for JJWT's HMAC key-length requirement and is not
  a production secret.
- `ForwardFilter` invokes the configured `UpstreamClient`, copies response
  status/body/headers, and terminates the chain. The app's
  `SimpleHttpUpstreamClient` applies configured connect and read timeouts;
  timeout exceptions propagate to the central mapper.
- `GatewayServletEntryPoint` now executes the `PipelineBuilder` chain and maps
  `GatewayException` subclasses centrally; it no longer converts arbitrary
  exceptions into success-shaped HTTP 500 responses.
- Malformed `RouteConfig` values now raise `InvalidConfigurationException`.

## Verification

Test files cover filter behavior, JWT token classes, route validation, and the
servlet authentication boundary. Execution results are pending:

```text
TODO(AN: paste terminal output for `mvn clean test`)
```

## Pre-P1-1 Coverage Datapoint — Not an M2 Baseline

The pre-P1-1 run is intended as a datapoint, not the M2 baseline. Its result is
not recorded until verified from pasted terminal output:

```text
Date: 22 November 2026
Command: mvn clean verify
Result: TODO(AN: paste terminal output)
```

The JaCoCo threshold and Maven quality configuration were not changed.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Describes the request pipeline, authentication boundary, exception mapping, and source changes. |
| SE431 | Links test sources and records execution/coverage evidence as pending. |
