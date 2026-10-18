# Publication changes

## Security fixes

- Removed database username/password and JWT signing-key fallbacks. Missing variables
  now prevent normal configuration; test-only H2 and synthetic JWT settings remain.
- Replaced shared deleted-task storage with user/scope keys; removed legacy unowned
  caches. Clear histories on logout/account change, reload on user/team changes and
  ignore in-flight deletion results after identity/scope changes.
- Removed seed/test datasets, Windows shortcuts and local/build artifacts from the
  candidate. Original source files and datasets remain untouched.

## Engineering changes

- Added frontend/backend environment references and restored script-only Maven Wrapper
  configuration. Kept the existing application architecture and Java business logic.
- Aligned fresh-install SQL with entities: team status, historical assignee metadata,
  dependency table, signed IDs, date/datetime fields and scalar historical log team ID.
  No migration was performed on an existing database. Application service rules remain.
- Kept pnpm as the single frontend lockfile; marked the package private, removed
  template release metadata and automatic hook installation. Preserved template license.
- Retained generated auto-import/component/route declarations in Git so fresh checkouts
  have the declarations needed by the initial vue-tsc build step.
- Added English portfolio documentation, attribution, validation record and cache tests.

## Behavior impact disclosed before changes

Old deleted-task display histories cannot safely be assigned to a user and are discarded.
Logout/account changes clear these browser caches. Server tasks, task deletion semantics,
role rules and dependency rules are unchanged. SQL applies to new databases only.

## Files added or changed

- Added: `docs/CHANGES.md`
- Added: `docs/PUBLICATION_CHECKLIST.md`

- Updated: `.gitignore`
- Updated: `README.md`
- Added: `THIRD_PARTY_NOTICES.md`
- Added: `backend/.env.example`
- Added: `backend/.mvn/wrapper/maven-wrapper.properties`
- Updated: `backend/README.md`
- Updated: `backend/src/main/resources/application.properties`
- Added: `docs/VALIDATION.md`
- Added: `docs/frontend-build-errors.txt`
- Added: `frontend/.env.example`
- Updated: `frontend/.gitignore`
- Updated: `frontend/README.md`
- Updated: `frontend/package.json`
- Updated: `frontend/src/pages/tasks/components/task-delete.vue`
- Updated: `frontend/src/pages/tasks/index.vue`
- Updated: `frontend/src/pages/teams/[teamId]/index.vue`
- Updated: `frontend/src/stores/auth.ts`
- Added: `frontend/src/utils/deleted-task-cache.ts`
- Added: `frontend/tests/deleted-task-cache.test.mjs`
- Updated: `sql/01_schema.sql`
- Updated: `sql/README.md`

## Intentionally omitted from the publication copy

- `sql/03_seed_data.sql`, `sql/04_test_data.sql`, `sql/init.sh`: seeded accounts/data and
  script that would import them.
- `frontend/package-lock.json`: redundant second package manager lockfile.
- `frontend/README-CN.md`, `frontend/CHANGELOG.md`, `frontend/vercel.json`: upstream
  marketing/release/deployment metadata, replaced by accurate project documentation.
- `.github` local content, build output, environment files, editor metadata and `.lnk` shortcuts.

## Deferred

47 existing frontend type errors, lint debt, disabled context test, browser interaction
verification and MySQL 8-specific validation. No type-check settings were weakened.
Team publication authorization remains unconfirmed. See `ASSET_CLEANUP.md` for
the subsequent asset cleanup and verified replacement icon provenance.
