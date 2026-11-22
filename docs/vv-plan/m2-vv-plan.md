# MAG Milestone 2 Verification and Validation Plan

## Purpose and scope

This plan covers M-A through M-E and uses the requirement IDs in the
[Milestone 1 RTM](../Milestone%201/Software%20Quality%20Assurance.tex).
**Verification (V)** checks implementation against specified requirements.
**Validation (Val)** checks that the integrated gateway serves the intended
user scenario. Planned activity is not evidence of execution.

## Planned activities

| Requirement | V / Val | Activity and method | Owner | Timing | Evidence |
|---|---|---|---|---|---|
| MAG-FR-01 | V | Exercise path/method matching, priority ordering, and 404 behavior with focused unit and integration cases. | AN | M2 | `TODO(AN: add route-priority test reference and output)` |
| MAG-FR-02 | V | Equivalence and boundary tests around configured request limits; concurrency stress test. | AA | M2 | `TODO(AA: provide algorithm and stress-test references/results)` |
| MAG-FR-03 | V | Check method/status cache eligibility and TTL boundaries with an injected clock. | JS | M2 | [CachingUpstreamClientTest.java](../../gateway-core/src/test/java/com/mag/core/upstream/impl/CachingUpstreamClientTest.java); execution `TODO(JS: paste output)` |
| MAG-FR-04 | V | Map each gateway exception subclass to its specified HTTP response. | AN | M2 | [GatewayExceptionMapperTest.java](../../gateway-core/src/test/java/com/mag/core/exception/GatewayExceptionMapperTest.java); execution `TODO(AN: paste output)` |
| MAG-QR-01 | V | Stress the limit under concurrent requests and inspect the excess-admission count. | AA | M2 | `TODO(AA: provide stress-test reference and output)` |
| MAG-QR-02 | V | Verify closed/open/half-open transitions and probe behavior using a fake clock. | JS | M2 | [CircuitBreakerTest.java](../../gateway-core/src/test/java/com/mag/core/resilience/CircuitBreakerTest.java); execution `TODO(JS: paste output)` |
| MAG-QR-03 | V | Exercise upstream timeout boundaries and verify timeout mapping and deadline behavior. | AN / JS | M2 | `TODO(AN/JS: provide timeout-boundary test reference and output)` |
| MAG-QR-04 | V | Submit invalid configuration and exercise snapshot publication under concurrent reads. | AA | M2 | `TODO(AA: provide configuration test reference and output)` |
| MAG-QR-05 | V | Test valid, expired, bad-signature, malformed, missing, and unsecured JWT classes. | AN / AA | M2 | [JjwtTokenVerifierTest.java](../../gateway-app/src/test/java/com/mag/app/auth/JjwtTokenVerifierTest.java) and [AuthFilterTest.java](../../gateway-core/src/test/java/com/mag/core/filter/impl/AuthFilterTest.java); execution `TODO(AN: paste output)` |
| MAG-QR-06 | V | Compare cache results across users and inspect logs for token disclosure. | JS | M2 | [CachingUpstreamClientTest.java](../../gateway-core/src/test/java/com/mag/core/upstream/impl/CachingUpstreamClientTest.java); log-scan evidence `TODO(JS: provide)` |
| MAG-QR-07 | V | Call protected admin endpoints with and without the credential. | AA | M2 | [GatewayIntegrationTest.java](../../gateway-app/src/test/java/com/mag/app/GatewayIntegrationTest.java); execution `TODO(AA: paste output)` |
| MAG-QR-08 | V | Measure gateway-added p99 latency under the specified load against a direct-upstream comparison. | AN / team | M3 | `TODO(AN: attach load-test plan, data, and environment)` |
| MAG-QR-09 | V | Check bounded eviction and idle-key behavior; run the planned soak test. | AA / JS | M3 | `TODO(AA/JS: provide soak-test reference and output)` |
| MAG-QR-10 | V | Generate a JaCoCo report, check the configured line-coverage gate, and scan unit tests for sleeps. | AN | M2 / M3 | `TODO(AN: paste terminal output and report path)` |
| MAG-QR-11 | V | Register an independent algorithm and run the shared algorithm contract. | AN / AA | M2 | [RateLimiterFactoryOcpTest.java](../../gateway-core/src/test/java/com/mag/core/ratelimit/RateLimiterFactoryOcpTest.java) and [RateLimitAlgorithmLspContractTest.java](../../gateway-core/src/test/java/com/mag/core/ratelimit/algorithm/RateLimitAlgorithmLspContractTest.java); execution `TODO(AN/AA: confirm)` |
| MAG-QR-12 | Val | Walk through browser login, redirects, cookies, and static assets via the gateway. | AN / team | M2 / M3 | `TODO(AN: record scenario, environment, and observed outcome)` |
| MAG-QR-13 | V | Submit valid and invalid admin payloads, including missing and malformed fields, and check field-specific 4xx responses. | AA | M2 | `TODO(AA: provide admin-validation test reference and output)` |

Test identifiers from the planned black-box and white-box design are not
assigned here. Add them when the responsible owners provide the real cases;
see [RTM v2](../rtm/rtm-v2.md).

## Review and completion

Record the command, environment, actual output, and artifact path for every
executed activity. A requirement is not accepted solely because a test is
planned or a source file exists. M2 activities verify component behavior;
M3 activities complete performance, soak, and integrated-user validation.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Verification methods are mapped to the gateway requirements and modules. |
| SE431 | The plan separates verification from validation, assigns timing/owners, and reserves evidence fields for actual results. |
