# Black-Box Test Design - Equivalence Partitioning and Boundary Value Analysis (P3-4)

Technique: black-box (specification-based). Test cases derive from requirements, not from code structure.

**Before submitting:** (1) replace every `MAG-??` with the real ID from the M1 RTM; (2) confirm the
test-method names marked *(confirm)* against the real test classes of AN / JS; (3) hand the final ID list to AN for RTM v2 (P3-7).
Known IDs from the plan: MAG-FR-02 (rate limit), MAG-FR-03 (cache), MAG-QR-05 (JWT), MAG-QR-13 (admin API).

## 1. Equivalence Partitioning

### 1.1 JWT token classes - MAG-QR-05
| TC ID | Req ID | Input partition | Representative input | Expected output | Test method |
|---|---|---|---|---|---|
| BB-JWT-01 | MAG-QR-05 | Valid | correctly signed, unexpired token | 200, request reaches upstream | `AuthFilterTest` valid-token case *(confirm)* (AN) |
| BB-JWT-02 | MAG-QR-05 | Expired | signed token with `exp` in the past | 401, upstream not called | `AuthFilterTest` expired case *(confirm)* |
| BB-JWT-03 | MAG-QR-05 | Bad signature | valid structure, signature altered | 401 | `AuthFilterTest` bad-signature case *(confirm)* |
| BB-JWT-04 | MAG-QR-05 | Malformed | `Bearer notajwt` | 401 (not 500) | `AuthFilterTest` malformed case *(confirm)*; extractor side: `KeyExtractorTest.malformedTokenGivesNullNotException` |
| BB-JWT-05 | MAG-QR-05 | Missing | no Authorization header | 401 | `AuthFilterTest` missing-token case *(confirm)* |
| BB-JWT-06 | MAG-QR-05 | `alg=none` | unsigned token with `"alg":"none"` | 401, never accepted | `AuthFilterTest` alg-none case *(confirm)* |

### 1.2 Rate-limit request count - MAG-FR-02 (limit N = 10 per window)
| TC ID | Req ID | Input partition | Input | Expected output | Test method |
|---|---|---|---|---|---|
| BB-RL-01 | MAG-FR-02 | 0 prior requests | first request of a new key | allowed | `RateLimitAlgorithmContractTest.firstRequestOfAnUnseenKeyIsAllowed` (x3 algorithms) |
| BB-RL-02 | MAG-FR-02 | 1 request (well below N) | request 1 of 10 | allowed | `RateLimitAlgorithmContractTest.oneRequestBelowTheLimitIsAllowedAndLeavesRoom` |
| BB-RL-03 | MAG-FR-02 | N-1 requests | request 9 | allowed | `RateLimitAlgorithmContractTest.boundaryNMinusOneNAndNPlusOne` |
| BB-RL-04 | MAG-FR-02 | N requests | request 10 | allowed | same method |
| BB-RL-05 | MAG-FR-02 | N+1 requests | request 11 | rejected, HTTP 429 | same method; at filter level `RateLimitGuardTest.shortCircuitsAfterLimit` |
| BB-RL-06 | MAG-FR-02 | Different identities | user A exhausted, user B sends | B allowed | `RateLimitAlgorithmContractTest.keysAreIndependent` |
| BB-RL-07 | MAG-FR-02 | Time passes | window elapsed after exhaustion | allowed again | `RateLimitAlgorithmContractTest.allowsAgainAfterEnoughTimePasses` |

### 1.3 Cache eligibility - MAG-FR-03
| TC ID | Req ID | Input partition | Input | Expected output | Test method |
|---|---|---|---|---|---|
| BB-CA-01 | MAG-FR-03 | GET / 200 | cacheable GET returning 200, repeated | 2nd call served from cache (upstream called once) | cache-hit test in `CachingUpstreamClientTest` *(confirm)* (JS) |
| BB-CA-02 | MAG-FR-03 | GET / non-200 | GET returning 500 | not cached; upstream called each time | "never serves non-200" test *(confirm)* |
| BB-CA-03 | MAG-FR-03 | non-GET / 200 | POST returning 200 | not cached | "never serves non-GET" test *(confirm)* |
| BB-CA-04 | MAG-FR-03 | non-GET / non-200 | POST returning 500 | not cached | same tests *(confirm)* |

### 1.4 Admin API payloads - MAG-QR-13
| TC ID | Req ID | Input partition | Input | Expected output | Test method |
|---|---|---|---|---|---|
| BB-AD-01 | MAG-QR-13 | Valid | complete valid route / rule | 201, change live and persisted | `AdminServiceTest.createRoutePersistsAndGoesLive` |
| BB-AD-02 | MAG-QR-13 | Missing required field | route without `pathPattern` | 400 naming `routes[0].pathPattern` | `AdminServiceTest.missingRequiredFieldIsRejected`; `ConfigValidatorTest.rejectsMissingPathPattern` |
| BB-AD-03 | MAG-QR-13 | Negative limit | rule with `limit = -1` | 400 naming `rules[0].limit` | `AdminServiceTest.negativeLimitIsRejectedWithFieldName` |
| BB-AD-04 | MAG-QR-13 | Malformed JSON | body `{not json` | 400, field `body` | `AdminApiHttpTest.malformedJsonGives400` (HTTP-level test; template provided - run it before citing it) |
| BB-AD-05 | MAG-QR-13 | No / wrong credential | no header / wrong token | 401 / 403 | `AdminCredentialCheckerTest`; `AdminApiHttpTest.noCredentialGives401`, `wrongCredentialGives403` |

## 2. Boundary Value Analysis

| TC ID | Req ID | Boundary | Input | Expected output | Test method |
|---|---|---|---|---|---|
| BVA-RL-01 | MAG-FR-02 | Rate limit exactly at N | N-th request in window | allowed | `boundaryNMinusOneNAndNPlusOne` |
| BVA-RL-02 | MAG-FR-02 | Rate limit at N+1 | (N+1)-th request in window | rejected (429) | same method |
| BVA-RL-03 | MAG-FR-02 | Window boundary | N requests at t=9.999 s, N more at t=10.000 s (limit 10 / 10 s) | fixed-window admits 20; token-bucket and sliding-window admit 10 | `EdgeBurstTest` (3 methods) |
| BVA-TTL-01 | MAG-FR-03 | TTL 1 ms before expiry | fake `Clock` at TTL - 1 ms | cache HIT | TTL-expiry test in `CachingUpstreamClientTest` *(confirm; may need an added boundary assertion)* |
| BVA-TTL-02 | MAG-FR-03 | TTL 1 ms after expiry | fake `Clock` at TTL + 1 ms | cache MISS, upstream called | same *(confirm)* |
| BVA-TTL-03 | MAG-FR-03 | TTL value 0 / -1 in config | `cacheTtlSeconds = 0` / `-1` | 0 accepted (caching off); -1 rejected | `ConfigValidatorTest.zeroTtlIsAllowedAndMeansNoCaching`, `rejectsNegativeTtl` |
| BVA-TMO-01 | MAG-?? (timeout) | At configured upstream timeout | upstream responds at exactly the timeout | per spec (document the chosen behaviour) | `ForwardFilterTest` timeout case *(confirm)* (AN) |
| BVA-TMO-02 | MAG-?? (timeout) | Timeout + 1 ms | upstream responds 1 ms after timeout | timeout error mapped to 504 | `ForwardFilterTest` timeout case *(confirm)* |

Total derived cases: 30 (EP: 22, BVA: 8, including the window-boundary case).

## 3. Traceability summary (feeds RTM v2)

| Requirement | Test case IDs |
|---|---|
| MAG-QR-05 | BB-JWT-01..06 |
| MAG-FR-02 | BB-RL-01..07, BVA-RL-01..03 |
| MAG-FR-03 | BB-CA-01..04, BVA-TTL-01..03 |
| MAG-QR-13 | BB-AD-01..05 |
| MAG-?? (timeout) | BVA-TMO-01..02 |
