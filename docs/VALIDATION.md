# Validation record

Run date: 2026-10-18 (Asia/Shanghai). These are local results, not deployment claims.

## Backend

- JDK: Eclipse Temurin 17.0.18; Maven 3.9.10; Spring Boot 3.5.12.
- Command: `JAVA_HOME=<JDK17> mvn test` in `backend/`.
- **102 discovered, 101 executed successfully, 0 failures, 0 errors, 1 skipped**.
- The existing `Lab1ApplicationTests.contextLoads` is disabled. It was not enabled
  or counted as a pass. Selected integration tests use H2, not MySQL.
- `JAVA_HOME=<JDK17> ./mvnw --version`: passed; restored wrapper resolves Maven 3.9.10.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Lab1ApplicationTests | 1 | 0 | 0 | 1 |
| AuthControllerTest | 2 | 0 | 0 | 0 |
| TaskControllerTest | 14 | 0 | 0 | 0 |
| TeamControllerTest | 7 | 0 | 0 | 0 |
| TeamTaskControllerTest | 9 | 0 | 0 | 0 |
| AuthServiceTest | 7 | 0 | 0 | 0 |
| TaskOperationLogEventListenerTest | 2 | 0 | 0 | 0 |
| TaskOperationLogIntegrationTest | 6 | 0 | 0 | 0 |
| TaskOperationLogServiceTest | 2 | 0 | 0 | 0 |
| TaskOperationLogWriterTest | 2 | 0 | 0 | 0 |
| TaskServiceTest | 38 | 0 | 0 | 0 |
| TeamServiceTest | 12 | 0 | 0 | 0 |

## MySQL smoke verification

MySQL **9.6.0** was reachable locally after allowing a connection outside the sandbox.
A uniquely named temporary database was created; both SQL files were executed with
only the database name substituted. The backend started using
`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, Java 17 and a temporary random JWT secret.

Passed checks:

- Schema and constraint initialization, JPA mapping validation and application startup.
- Unauthenticated task access rejected; registration/login for two synthetic users.
- Personal task creation, cross-user read denial and operation-log retrieval.
- Dependency insertion and reverse-edge cycle rejection.
- Team and team task creation.

The final run used an isolated port with proxy bypass. Its backend process was
stopped and its temporary database dropped. No existing course database was modified.
This is a small API smoke check, **not the full suite running against MySQL**, not
browser E2E, and not a MySQL 8 compatibility certification.

## Frontend

- Node 24.10.0; pnpm 10.32.1 (project package-manager pin).
- `pnpm install --frozen-lockfile --ignore-scripts`: passed after network access retry.
- `pnpm build`: **failed** at `vue-tsc` with **47 existing diagnostics**.
  An unmodified source copy checked against the same installed dependencies produced
  the identical diagnostics after normalizing line positions; no additional TypeScript
  diagnostics were introduced by this cleanup.
- Errors are unused imports/declarations and two generic form-value incompatibilities
  in `src/composables/use-system-config.ts`. See `frontend-build-errors.txt`.
- `pnpm exec vite build`: passed outside the sandbox after an initial EMFILE watcher
  failure. Two ineffective dynamic-import warnings remain. This standalone bundle
  result does **not** mean the standard `pnpm build` passes.
- `pnpm lint:fix`: failed with 78 errors and 11 warnings remaining after autofixes.
  Broad automatic formatting was reverted to keep this MVP focused; this is not a
  claim that the current tree is lint-clean.
- `node --experimental-strip-types --test tests/deleted-task-cache.test.mjs`:
  **6 passed, 0 failed** (user/team isolation, anonymous sessions, legacy-cache removal,
  malformed storage, logout cleanup and unavailable storage).

## Testing notes / remaining manual checks

No automated browser runner existed; the new tests exercise the cache helper only.
Manually verify deletion from both personal-task entry points, team deletion, logout,
account switching, team navigation and an account switch while deletion is in flight.
The cache is local display history, not a protected server archive. Standard frontend
build/type cleanup remains an engineering blocker for calling the MVP fully buildable.
Configuration/staging checks are recorded in `PUBLICATION_CHECKLIST.md`.
