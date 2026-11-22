# MAG Requirements Traceability Matrix — M2 v2

This update retains the requirement identifiers and intent from the
[M1 SQA/RTM](../Milestone%201/Software%20Quality%20Assurance.tex). Status is a
source-level assessment, not a test result. Assigned test IDs, inspection
findings, and coverage reports are not available here and remain visible
TODOs.

| Requirement | Summary | M1 module | Implementation status | Test IDs from AA / JS | Coverage evidence |
|---|---|---|---|---|---|
| MAG-FR-01 | Highest-priority matching route wins; otherwise 404. | M-A | Implemented in source; execution evidence pending. | `TODO(AA/JS: add assigned black-box/white-box test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-FR-02 | Allow up to N requests; reject N+1 with 429 and Retry-After. | M-B | Not implemented: limiter strategy classes are stubs. | `TODO(AA/JS: add assigned test IDs after implementation)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-FR-03 | Cache only GET/200 and never serve after TTL. | M-C | Implemented in source; execution evidence pending. | `TODO(AA/JS: add assigned black-box/white-box test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-FR-04 | Map gateway errors centrally to the specified HTTP statuses. | M-A | Implemented in source; execution evidence pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-01 | No over-limit admissions under 100 concurrent threads. | M-B | Not implemented: limiter strategies are stubs. | `TODO(AA/JS: add stress-test ID and result)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-02 | Circuit opens, fails fast, then recovers or re-opens after probes. | M-D | Implemented in source; execution evidence pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-03 | Hanging upstream returns 504 within timeout plus 200 ms. | M-A / M-D | Partial: timeout handling exists; the required boundary evidence is pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-04 | Invalid config leaves prior snapshot active; reload loses no requests. | M-E | Partial: snapshot store and admin components exist; complete validation/reload evidence is pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-05 | Invalid, missing, and unsecured JWTs are rejected with 401. | M-B | Implemented in source; execution evidence pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-06 | No cross-user cache response; tokens do not appear in logs. | M-C | Partial: cache exists; cross-user and log-scan evidence is pending. | `TODO(AA/JS: add assigned security-test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-07 | Admin API rejects calls without the admin credential. | M-E | Partial: admin API exists; credential enforcement evidence is pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-08 | Gateway adds at most 10 ms p99 at 500 req/s. | All | Partial: implementation exists; calibrated load evidence is pending. | `TODO(AA/JS: add assigned load-test ID)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-09 | Cache stays bounded and idle client keys are evicted. | M-B / M-C | Partial: bounded cache source exists; limiter-key eviction and soak evidence are pending. | `TODO(AA/JS: add assigned soak-test ID)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-10 | Core line coverage is at least 60%; unit tests use no sleeps. | All | Partial: JaCoCo is configured; actual coverage and sleep-scan evidence are pending. | `TODO(AA/JS: add assigned test/scan references)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-11 | Add an algorithm with a class and registration, without edits to existing algorithms. | M-B | Partial: factory registry exists; strategies are stubs. | `TODO(AA/JS: add assigned OCP/LSP test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-12 | Browser login through gateway works with cookies, redirects, and assets. | M-A | Partial: scenario execution and compatibility evidence are pending. | `TODO(AA/JS: add assigned scenario ID)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |
| MAG-QR-13 | Invalid admin payload returns 4xx naming field and reason. | M-E | Partial: admin API exists; field-level validation evidence is pending. | `TODO(AA/JS: add assigned test IDs)` | `TODO(AA/JS: provide accepted JaCoCo evidence reference)` |

## Inspection defects and CAPA

Inspection findings have not been supplied. Do not infer defects from these
implementation-status notes. Populate the table from the actual P3-1 report.

| Defect ID | Inspection finding / evidence | Correction and corrective action | Owner | Status | Closure evidence |
|---|---|---|---|---|---|
| `TODO(AA/JS: populate from inspection report; no defect ID assigned here)` | `TODO(AA/JS: provide finding and report reference)` | `TODO(AA/JS: record approved action)` | `TODO(AA/JS: assign)` | `TODO(AA/JS: Open / In Progress / Closed)` | `TODO(AA/JS: link actual fix evidence)` |

No defects are marked closed without a source finding and closure evidence.

## Rubric mapping

| Course | Evidence |
|---|---|
| SE423 | Traces each M1 functional and quality requirement to source-level status and its module. |
| SE431 | Provides v2 traceability, evidence placeholders, and a CAPA structure without fabricating IDs or results. |
