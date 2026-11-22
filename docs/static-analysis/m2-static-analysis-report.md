# M2 Static Analysis Report

Audit date: 2026-11-22

## Scope and environment

- Initially audited revision: `2780b4c` (plus local M-C/M-D fixes)
- Final evidence commit: `7913638` (`Document M2 evidence and harden core`, including M-C/M-D fixes and reports)
- Modules: `gateway-core`, `gateway-app`
- Maven: 3.9.11
- Java runtime: Oracle JDK 24.0.2
- SpotBugs Maven plugin: 4.8.6.4
- SpotBugs engine: 4.8.6
- Checkstyle Maven plugin: 3.5.0
- Checkstyle engine: 9.3 (as recorded in the generated XML)

## Commands

```powershell
mvn -pl gateway-core checkstyle:check
mvn -pl gateway-app checkstyle:check
mvn -pl gateway-core checkstyle:checkstyle spotbugs:spotbugs
mvn -pl gateway-app checkstyle:checkstyle spotbugs:spotbugs
mvn -pl gateway-core test checkstyle:check
```

The first Checkstyle runs passed; the final core test + Checkstyle run passed 30 tests and had 0 violations.

## Machine-generated reports

- Core Checkstyle XML: [raw/gateway-core-checkstyle.xml](raw/gateway-core-checkstyle.xml)
- App Checkstyle XML: [raw/gateway-app-checkstyle.xml](raw/gateway-app-checkstyle.xml)
- Core SpotBugs attempt XML: [raw/gateway-core-spotbugs.xml](raw/gateway-core-spotbugs.xml)
- App SpotBugs attempt XML: [raw/gateway-app-spotbugs.xml](raw/gateway-app-spotbugs.xml)
- Core tests + Checkstyle log: [raw/gateway-core-tests-checkstyle.log](raw/gateway-core-tests-checkstyle.log)

## Findings

### Checkstyle

| ID | Rule | Severity | File | Line | Description | Category | Resolution |
|---|---|---|---|---:|---|---|---|
| CS-CORE-1 | `UnusedImportsCheck` | Warning | `gateway-core/src/main/java/com/mag/core/filter/impl/RequestIdFilter.java` | 7 | Unused import `java.util.UUID` | Fixed | Removed in the working tree; core Checkstyle rerun reports 0 violations. |
| CS-APP-1 | `UnusedImportsCheck` | Warning | `gateway-app/src/main/java/com/mag/app/auth/JjwtTokenVerifier.java` | 4 | Unused import `com.mag.core.exception.AuthenticationException` | Deferred | Owned by another member; no change made to `gateway-app` as requested. |
| CS-APP-2 | `CyclomaticComplexityCheck` | Warning | `gateway-app/src/main/java/com/mag/app/servlet/GatewayServletEntryPoint.java` | 55 | Cyclomatic complexity 21 exceeds 10 | Deferred | The finding belongs to application integration code outside M-C/M-D; no change made to other members' code. |
| CS-APP-3 | `CyclomaticComplexityCheck` | Warning | `gateway-app/src/main/java/com/mag/app/upstream/SimpleHttpUpstreamClient.java` | 39 | Cyclomatic complexity 11 exceeds 10 | Deferred | The finding belongs to application upstream code outside M-C/M-D; no change made to other members' code. |

Totals:

- Core: 1 finding before fix, 0 findings after fix.
- App: 3 findings; 0 fixed, 3 deferred.
- Checkstyle triage: 1 Fixed, 0 False Positive, 3 Deferred.

### SpotBugs

SpotBugs did not produce usable findings. Both module runs failed while analyzing JDK 24 class files:

`IllegalArgumentException: Unsupported class file major version 68`

The raw XML files are analysis-error records, not successful SpotBugs finding reports. They contain 152 core and 152 app-level analysis errors respectively. This means no SpotBugs rule/file/line finding can be truthfully reported or triaged. No JDK 21 download or environment change was made per the user's instruction.

## SpotBugs rerun procedure

1. Install or select JDK 21 for the Maven process. The current machine's JDK 24 class files are major version 68 and cannot be analyzed by SpotBugs 4.8.6.
2. Set `JAVA_HOME` to JDK 21 and place `%JAVA_HOME%\bin` first in `PATH`.
3. Compile before analysis:

```powershell
mvn -pl gateway-core,gateway-app -DskipTests clean compile
mvn -pl gateway-core spotbugs:spotbugs
mvn -pl gateway-app spotbugs:spotbugs
```

4. For the configured verify-phase plugin, if it is available for the module:

```powershell
mvn -pl gateway-core,gateway-app -DskipTests verify
```

5. Preserve the generated HTML/XML reports and rerun the triage table from machine findings only. Do not classify JDK incompatibility as a source finding.

The rerun should target JDK 21, not JDK 24. If JDK 21 is unavailable, state this as an environment blocker rather than claiming an unsuccessful JDK 24 run proves the source is clean.

## Genuine M-C/M-D fixes

| ID | Module | File | Defect | Fix | Verification |
|---|---|---|---|---|---|
| MC-1 | M-C | `gateway-core/src/main/java/com/mag/core/upstream/impl/CachingUpstreamClient.java` | An oversized or malformed numeric `Cache-Control: max-age` value could throw `NumberFormatException` or overflow TTL arithmetic and fail a valid upstream response. | Catch `NumberFormatException` and `ArithmeticException` and fall back to the configured default TTL. | Added `testOversizedMaxAgeFallsBackToDefaultTtl`; core suite passed. |
| MD-1 | M-D | `gateway-core/src/main/java/com/mag/core/upstream/impl/ResilientUpstreamClient.java` | A null upstream response produced an uncategorized null dereference instead of recorded breaker failure and a gateway unavailable result; `Throwable` handling also converted JVM `Error` subclasses. | Return a 502 `UpstreamUnavailableException`, record failure, catch only `RuntimeException`, and explicitly propagate `Error`. | Added `testNullUpstreamResponse`; core suite passed. |

The fixes and their regression tests are included in evidence commit `7913638`; no separate fixing commit SHA was created before the combined commit. No code outside M-C/M-D was changed for these fixes.
