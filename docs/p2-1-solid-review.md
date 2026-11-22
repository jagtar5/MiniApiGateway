# P2-1 — OCP, LSP, and SOLID Review Notes

## Contract scope

- `UpstreamClientLspContractTest` exercises the three core `UpstreamClient`
  implementations through `ForwardFilter`: cache decorator, resilience
  decorator, and the locked HTTP placeholder.
- `SimpleHttpUpstreamClientLspContractTest` exercises the app's local HTTP
  adapter through the same filter boundary.
- `RateLimiterFactoryOcpTest` registers an independent no-op strategy through
  the factory registry; it does not edit an existing algorithm.
- `RateLimitAlgorithmLspContractTest` defines shared valid-input, limit, and
  non-negative retry contracts for the three rate-limit strategies.

The rate-limit classes are currently skeletons. Their contract cases abort
only when the exact `implement in M2` stub exception is raised; other
unsupported-operation failures remain test errors. In the current
`mvn clean verify` run, all three rate-limit cases were skipped pending AA's
implementations. No AA implementation files were changed.

## Review notes

| Principle | Evidence / review note |
|---|---|
| SRP | Request-ID, authentication, forwarding, and exception mapping have separate responsibilities. |
| OCP | The rate-limiter factory registers creators by name; the new-strategy proof uses that extension point. |
| LSP | `ForwardFilter` depends on `UpstreamClient`; the contract test checks response preservation across the core implementations. |
| ISP | `GatewayFilter` and `UpstreamClient` expose one focused operation each. |
| DIP | `ForwardFilter` receives the `UpstreamClient` abstraction, not a concrete transport or decorator. |

No Git diff or issue check was performed. No runtime or test result is asserted
here.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | OCP proof, LSP contracts, and SOLID review notes support extensibility and substitutability. |
| SE431 | Contract tests provide planned verification evidence; execution status remains unverified. |
