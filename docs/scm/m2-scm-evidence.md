# M2 SCM Quality-Gate Evidence

Audit date: 2026-11-22

## Branching strategy

The intended GitHub Flow variant is short-lived feature branches from `main`, reviewed PRs, and a stable merged `main`. Actual evidence shows `main`, feature branches, and one merged PR, but only the JS integration branch is represented in the available merged result. Branch protection and team-wide branch completeness are not demonstrated.

## Merge and quality gates

| Gate | Required rule | Actual evidence | Status |
|---|---|---|---|
| Pull request | Feature work merges through PR | PR #1 merged feature branch to main | Partial |
| Non-author reviewer | Reviewer not the PR author | No reviews returned for PR #1 | Missing |
| Main protection | Main protected, CI required | Team reports protection enabled; save GitHub screenshot/settings export | Partially evidenced; independent proof pending |
| CI before merge | Build/tests/static analysis green | No GitHub Actions workflows found | Missing |
| Commit convention | `test:/feat:/refactor:/fix:/docs:` prefixes | Some JS commits conform; other commits and historical work do not | Partial/deviation |
| Preserve red-green trail | Squash merge disabled | No repository setting evidence found | Missing/undetermined |

## Repository evidence

### Merged pull requests

Two merged PRs currently exist:

1. [PR #1 — Feature/js integration](https://github.com/jagtar5/MiniApiGateway/pull/1), merged 2026-11-21, author `jagtar5`, reviewer evidence: none.
2. [PR #2 — Document M2 evidence and harden core](https://github.com/jagtar5/MiniApiGateway/pull/2), merged 2026-11-22, author `jagtar5`, reviewer evidence: none.

Only two real merged PRs could be identified; at least one additional merged PR is required to meet the explicit M2 plan target. Branch inspection alone does not prove merged PRs.

### Reviewer evidence

GitHub returned no reviews or comments for PR #1 or PR #2. Therefore no non-author reviewer approval can be recorded for this PR.

### Commit/branch evidence

- PR #1 merge commit: https://github.com/jagtar5/MiniApiGateway/commit/c4a5e43
- PR #2 head/document commit: https://github.com/jagtar5/MiniApiGateway/commit/7913638
- Integration head: `2780b4c`
- Cache implementation: https://github.com/jagtar5/MiniApiGateway/commit/8094301
- Cache tests: https://github.com/jagtar5/MiniApiGateway/commit/9b78180
- Resilience implementation: https://github.com/jagtar5/MiniApiGateway/commit/ef37bd6
- Resilience tests: https://github.com/jagtar5/MiniApiGateway/commit/a7b6f1c
- Integration tests: https://github.com/jagtar5/MiniApiGateway/commit/251275c
- Dashboard/integration: https://github.com/jagtar5/MiniApiGateway/commit/56cf134

### CI runs

GitHub Actions list operation returned `total_count: 0`. No workflow, run, artifact or CI success evidence exists in the repository as currently exposed.

### Baseline tag

No `m2-baseline` tag exists locally or remotely. `m1-baseline` exists but does not satisfy the M2 baseline requirement. No tag was created or moved.

## Commit-prefix log

Conforming M-C/M-D examples:

- `test: add unit and concurrency test suite for M-C Fast-Path Cache`
- `feat: implement M-C Fast-Path Cache with BoundedLruCache, TTL, and single-flight`
- `test: add state machine and resilience tests for M-D CircuitBreaker`
- `feat: implement M-D Resilience Engine with State pattern and atomic CAS`
- `test: add integration test suite for gateway shell and admin dashboard API`
- `feat: complete M2 P1-5 integration decorator chain and three-panel demo dashboard`

Nonconforming examples include `Implement closed circuit failure window`, `Add dummy upstream test fixtures`, and earlier history such as `minor errors`, `documents`, and `editted`.

## How to obtain the remaining PR evidence

1. Create a small, meaningful branch from `main`, for example `feature/aa-thread-safety-doc` or `docs/js-final-evidence`.
2. Commit the actual work using an allowed prefix (`feat:`, `test:`, `fix:`, `docs:`, or `refactor:`).
3. Open a GitHub PR to `main`, link the relevant issue/task and request a review from a different team member.
4. Do not merge before the reviewer approves. Preserve the review and merged-commit link.
5. Repeat until at least three real merged PRs exist with actual non-author reviewer names. PRs for documentation-only work are acceptable only if they contain meaningful, attributable M2 work.

## High-priority remaining SCM work

- Save a screenshot or settings export proving `main` branch protection and required status checks if CI is later configured.
- Add at least one workflow run; at minimum build and execute Maven tests before merge.
- Add an annotated `m2-baseline` tag only after CI and team approval, and preserve the tag SHA and reproduction command.

## Remaining blockers

- Add at least one other real merged PR with a non-author reviewer.
- Retain branch-protection proof and configure CI.
- Configure and record squash merge policy (or retain merge commits if not violating team policy).
- Resolve commit-prefix deviations or document whether historical commits are excluded.
- Obtain team approval for a `m2-baseline` tag after a green CI run.
