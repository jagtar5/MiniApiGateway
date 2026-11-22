# ADR-003: Atomic Configuration Snapshot Swap

| Field | Value |
|---|---|
| Status | Proposed for M2 review |
| Scope | Publishing configuration to concurrent request handlers |

## Context

Configuration updates must not expose a partially updated set of values to
request handlers. A lock can protect reads and writes, but it couples each
request to update coordination.

## Decision

Build a complete immutable configuration snapshot, then publish it atomically.
Readers obtain a consistent snapshot without holding an update lock. The
current store is [AtomicConfigStore.java](../../gateway-app/src/main/java/com/mag/app/config/AtomicConfigStore.java);
the snapshot type is [ConfigSnapshot.java](../../gateway-core/src/main/java/com/mag/core/config/ConfigSnapshot.java).

## Rejected alternative

Mutate shared configuration fields under a read/write lock. This adds lock
coordination to reads and risks inconsistent reads if any access bypasses the
lock.

## Verification

Verify readers see either the old or the complete new snapshot during repeated
concurrent publication, and invalid candidates are not published. Test
references/output: `TODO(AA: provide actual test reference and output)`.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Documents the consistency problem, atomic publication choice, and rejected locking approach. |
| SE431 | Defines concurrent-reader and invalid-update verification obligations. |
