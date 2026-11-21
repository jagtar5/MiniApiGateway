# M2 SCM Quality-Gate Evidence

Audit date: 2026-11-22

## Branching strategy

The intended GitHub Flow variant is short-lived feature branches from `main`, reviewed PRs, and a stable merged `main`. Actual evidence shows `main`, feature branches, and one merged PR, but only the JS integration branch is represented in the available merged result. Branch protection and team-wide branch completeness are not demonstrated.

## Merge and quality gates

| Gate | Required rule | Actual evidence | Status |
|---|---|---|---|
| Pull request | Feature work merges through PR | PR #1 merged feature branch to main | Partial |
| Non-author reviewer | Reviewer not the PR author | No reviews returned for PR #1 | Missing |
| Main protection | Main protected, CI required | GitHub API reports `main` unprotected | Missing |
| CI before merge | Build/tests/static analysis green | No GitHub Actions workflows found | Missing |
| Commit convention | `test:/feat:/refactor:/fix:/docs:` prefixes | Some JS commits conform; other commits and historical work do not | Partial/deviation |
| Preserve red-green trail | Squash merge disabled | No repository setting evidence found | Missing/undetermined |

## Repository evidence

### Merged pull requests

Only one merged PR currently exists:

1. [PR #1 — Feature/js integration](https://github.com/jagtar5/MiniApiGateway/pull/1), merged 2026-11-21, author `jagtar5`, reviewer evidence: none.

At least three real merged PRs could not be identified. Branch inspection alone does not prove merged PRs.

### Reviewer evidence

GitHub returned no reviews or comments for PR #1. Therefore no non-author reviewer approval can be recorded for this PR.

### Commit/branch evidence

- Merge commit: https://github.com/jagtar5/MiniApiGateway/commit/c4a5e43
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

## Remaining blockers

- Add at least two other real merged PRs with non-author reviewers.
- Configure branch protection and CI.
- Configure and record squash merge policy (or retain merge commits if not violating team policy).
- Resolve commit-prefix deviations or document whether historical commits are excluded.
- Obtain team approval for a `m2-baseline` tag after a green CI run.
