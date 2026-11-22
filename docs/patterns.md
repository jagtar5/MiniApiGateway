# M2 Pattern Rationale

Patterns below describe intended roles in this gateway. Test links identify
verification source, not a claim that the tests have been run.

| Pattern | Problem solved | Rejected alternative | How to verify |
|---|---|---|---|
| Chain of Responsibility | Composes request-ID, auth, and forwarding filters without putting all request policy into one servlet handler. | One large handler with interleaved concerns. | Exercise filter ordering and response behavior in [GatewayIntegrationTest.java](../gateway-app/src/test/java/com/mag/app/GatewayIntegrationTest.java); execution `TODO(AN: paste output)`. |
| Strategy | Allows rate-limit algorithms to share a contract and be selected independently. | Conditional algorithm branches embedded in the request filter. | Apply [RateLimitAlgorithmLspContractTest.java](../gateway-core/src/test/java/com/mag/core/ratelimit/algorithm/RateLimitAlgorithmLspContractTest.java) after AA's strategies are implemented; current implementations are stubs. |
| State | Represents circuit-breaker states and transitions explicitly. | Scattered booleans and unrelated threshold checks. | Verify transitions and probe handling in [CircuitBreakerTest.java](../gateway-core/src/test/java/com/mag/core/resilience/CircuitBreakerTest.java); execution `TODO(JS: paste output)`. |
| Factory / registry | Resolves a named rate-limit strategy while permitting registration of another implementation. | Hard-coded construction branches in the filter. | Register an independent strategy in [RateLimiterFactoryOcpTest.java](../gateway-core/src/test/java/com/mag/core/ratelimit/RateLimiterFactoryOcpTest.java); execution `TODO(AN: paste output)`. |
| Proxy / decorator | Adds cache and resilience behavior around an `UpstreamClient` without changing the forwarding boundary. | Couple caching and circuit-breaker policy to `ForwardFilter`. | Contract-test clients through [ForwardFilter](../gateway-core/src/main/java/com/mag/core/filter/impl/ForwardFilter.java) with [UpstreamClientLspContractTest.java](../gateway-core/src/test/java/com/mag/core/filter/impl/UpstreamClientLspContractTest.java); execution `TODO(AN: paste output)`. |
| Builder | Creates configured filter chains without exposing construction details to the servlet entry point. | Construct and wire every filter inline in the servlet. | Check configured pipeline behavior in [GatewayIntegrationTest.java](../gateway-app/src/test/java/com/mag/app/GatewayIntegrationTest.java); execution `TODO(AN: paste output)`. |

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Connects each pattern to a gateway design problem and rejected alternative. |
| SE431 | Provides testable verification paths and explicitly separates test source from execution evidence. |
