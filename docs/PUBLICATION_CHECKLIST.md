# Publication checks

Status: **LOCAL CANDIDATE ONLY — not cleared for public release**.

## Addressed security issues

- Production configuration contains no default database password or JWT signing key.
  DB username/password and JWT secret must be supplied externally.
- Personal and team deleted-task caches are isolated by user, with logout cleanup,
  legacy-cache removal and guards against stale deletion callbacks.
- Seed accounts, password hashes from seed data, course datasets, local environment
  files, Windows shortcuts and build/dependency outputs are not staged.
- Required environment examples and Maven Wrapper properties are staged and are not
  accidentally hidden by ignore patterns.

## Checked publication contents

- Filename checks: no `node_modules`, `target`, `dist`, local environment files,
  private-key files or shortcuts in the index.
- Pattern scan of indexed text files: no private-key blocks, GitHub token patterns,
  AWS access-key patterns or literal JWTs found. Manual review of credential-related
  configuration found required environment placeholders and synthetic test fixtures.
- The fixed JWT string in `backend/src/test/resources/application-test.properties`
  is a public test-only fixture, not a runtime secret. Never use it outside tests.
- Frontend example email `m@example.com` is template placeholder data. No real course
  seed accounts or personal datasets are included.
- `frontend/LICENSE` is byte-for-byte identical to the supplied MIT notice.
- Original source-file hashes are unchanged. No source history was copied.
- `git remote -v` is empty. No remote repository was created and no push was run.
- `git diff --cached --check` flags existing trailing whitespace in the imported
  source. This is recorded as formatting debt; broad reformatting was avoided.

These checks are scoped source/index checks, not a comprehensive security audit or
a guarantee that all dependencies and assets are safe or legally cleared.

## Publication blockers

1. Team permission to publish the original code has not been established.
2. No project-wide license has been selected for the team's code. The original
   frontend template revision remains unrecorded; its supplied MIT notice is retained.
   The questioned bundled assets have now been removed or replaced with a verified
   Lucide icon; see `ASSET_CLEANUP.md`.

## Engineering limitations (separate from publication safety)

- `pnpm build` fails with 47 pre-existing TypeScript diagnostics; Vite-only bundling passes.
- Lint debt, existing whitespace and the disabled context-load test remain.
- No browser E2E verification; cache helpers alone have automated regression coverage.
- MySQL 9.6 smoke checks passed, but the full suite has not run against MySQL, and
  MySQL 8-specific compatibility has not been tested.
- The original local-development `ddl-auto=update`, CORS origins and browser token
  persistence remain. There is no production-hardening or deployment claim.

## Git handoff

The local repository is initialized on `main` with candidate files staged for review.
No initial commit was created, so there is no invented author identity or unreviewed
public history. The user can review staged changes and choose a commit identity before
committing. A remote and push require explicit user confirmation in a later phase.
