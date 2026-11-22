# Thread-Safety Strategy (M2) - P2-3

Format follows the M1 table: **hazard -> strategy -> verification**. Please align wording with the
M1 document before submitting; rows for modules owned by other members were written from the
M2 plan and must be confirmed by their owners.

| # | Component | Owner | Hazard | Strategy | Verification (test, runs in CI) |
|---|---|---|---|---|---|
| 1 | `TokenBucketAlgorithm` | AA | Lost update / check-then-act: two threads read the same token count and both consume it, admitting more than the limit | Whole read-refill-consume-write cycle inside `ConcurrentHashMap.compute` (atomic per key); bucket state is an immutable record, never mutated in place | `RateLimitConcurrencyStressTest.tokenBucketHasZeroLeaks` - 100 threads x 5 attempts, limit 100, frozen clock, leak = 0 |
| 2 | `FixedWindowAlgorithm` | AA | Same check-then-act on the counter; window rollover racing with increments | `compute` per key; window id and count held together in one immutable record, so rollover and increment are one atomic step | `RateLimitConcurrencyStressTest.fixedWindowHasZeroLeaks` |
| 3 | `SlidingWindowCounterAlgorithm` | AA | Check-then-act across current + previous counters | `compute` per key; both counters and the window id swapped together as one record | `RateLimitConcurrencyStressTest.slidingWindowHasZeroLeaks` |
| 4 | `RateLimiterRegistry` | AA | Reader sees a half-built rule set during hot reload | Immutable map held in an `AtomicReference`; `replaceAll` builds the new map completely, then swaps (copy-on-write). Writers serialised with `synchronized`; readers lock-free | `RateLimitGuardTest.unchangedRuleKeepsItsCountersAcrossReload`; `RateLimiterRegistryRefresherTest` |
| 5 | `ConfigManager` / `ConfigSnapshot` | AA | Request thread reads a config that is being replaced (torn read); invalid config partially applied | `ConfigSnapshot` is an immutable record with defensive list copies; `AtomicReference.set()` is the only write path; validation happens before the swap; readers take no lock | `ConfigManagerTest.hotReloadUnderConcurrentReadersDropsNoReads` (20 readers, 200 swaps); `ConfigManagerTest.lastKnownGoodIsKeptWhenNewConfigIsInvalid`; `ConfigManagerTest.snapshotIsImmutable` |
| 6 | `AdminService` | AA | Two admin calls interleave read-modify-write of the snapshot (lost update) | Mutating methods are `synchronized`; each one reads the current snapshot, builds a candidate, validates, persists, then swaps | `AdminServiceTest` (sequential behaviour); concurrent admin writes are expected to be rare - see residual risk |
| 7 | `CachingUpstreamClient` | JS | Cache stampede: N concurrent misses on one key all hit the upstream | Single-flight via `ConcurrentHashMap<Key, CompletableFuture>` (confirm with JS) | single-flight test in P1-3a - 100 concurrent misses -> 1 upstream call (confirm exact name) |
| 8 | `CircuitBreaker` | JS | Concurrent failures cause several Closed->Open transitions | State held in an atomic reference, transition by compare-and-set (confirm with JS) | concurrent failure test in P1-3b - 10 concurrent failures -> exactly one OPEN transition (confirm exact name) |

## Residual risks (honest list)

- Per-key maps in the three algorithms grow with the number of distinct keys and are never evicted. A
  flood of unique keys (e.g. many IPs) could use memory. Candidate for an M3 improvement (idle-key eviction).
- Admin writes are serialised in one JVM only. Running two gateway instances against the same database
  would need DB-level locking or optimistic versioning.
- `ConfigManager` listeners run on the reloading thread; a slow listener delays the admin response
  (not the request path).

## Ownership summary

AA: rows 1-6. JS: rows 7-8. AN: pipeline is stateless per request (no shared mutable state) - confirm in review.
