# Team Task Platform

## Project Overview

A personal and team task collaboration platform developed by a software engineering
course team. This repository preserves the working Spring Boot backend and Vue
frontend, with a focus on API integration, authorization and task business rules.
It is a portfolio candidate, not a claim of production deployment.

## Features

- Registration and login with BCrypt password hashing and JWT access/refresh tokens.
- Personal and team task creation, updates, deletion, status and priority management.
- Owner/Admin/Member roles, membership management and task assignment.
- Predecessor/successor dependencies, cycle detection and dependency-aware completion.
- Task operation logs, including changes to assignment and dependencies.
- Browser-side deleted-task history, scoped to the signed-in user and team; it is
  a display cache, not a server-side recycle bin or restore feature.

These are **team project features**, not a list of one person's contributions.
Some template demo screens remain and should not be interpreted as implemented
backend features.

## Tech Stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.12, Spring Web, Spring Data JPA, Hibernate |
| Database | MySQL; H2 for selected automated tests |
| Authentication | JJWT, Spring Security Crypto (BCrypt) |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Axios, TanStack Vue Query |
| UI foundation | shadcn-vue-admin, shadcn-vue, Tailwind CSS |
| Tests/build | JUnit 5, Mockito, Spring Boot Test, Maven, pnpm, vue-tsc |

## Backend Architecture

The backend uses a controller–service–repository structure under
`backend/src/main/java/com/group15/lab1`:

- `controller` and `controller/dto`: HTTP endpoints, request validation and responses.
- `service/impl`: task lifecycle, team roles, assignment, dependency traversal and
  cycle checks. Operation-log events are handled by dedicated listeners/writers.
- `repository` and `entity`: Spring Data JPA persistence and domain mappings.
- `security`: token creation, signature validation and claim extraction.
- `exception`: application errors and centralized response handling.

Protected controllers extract the user from the access token; services enforce
ownership and team permissions. The frontend sends Bearer tokens through Axios.
This is the existing course architecture, not a claim of a Spring Security filter chain.

```text
backend/                  Spring Boot application and existing tests
  .mvn/wrapper/           Maven Wrapper configuration
  src/main/               Application code and environment-based configuration
  src/test/               Controller, service and H2 integration tests
frontend/                 Vue application and template license
  .env.example            Public API configuration
  src/api/                API clients and DTO types
  src/pages/              Task, team, authentication and template pages
  src/stores/             Pinia session state
sql/                      Fresh-database schema and constraints (no seed users)
docs/                     Validation, changes and publication checks
```

## API Overview

| Base path | Main operations |
| --- | --- |
| `/api/auth` | `POST /register`, `/login`, `/refresh`, `/validate`; `GET /me` |
| `/api/tasks` | Personal task CRUD, `GET /my`, task logs and dependencies |
| `/api/teams` | Create/list/read teams, manage members/roles, transfer ownership, disband |
| `/api/teams/{teamId}/tasks` | Team task CRUD, assignment, status, logs and dependencies |

Task subresources include `/{id}/logs`, `/{id}/dependencies/predecessors`,
`/{id}/dependencies/successors` and dependency creation/removal. See controller
DTOs for the exact request contracts. Protected calls require
`Authorization: Bearer <access-token>`; the auth token validation route is POST,
not a health endpoint.

## Setup and Usage

Prerequisites: JDK 17, MySQL 8.0.16+, Node.js 22.15+ and pnpm 10. The package
manager is pinned in `frontend/package.json`. Maven Wrapper downloads Maven
3.9.10 on first use; installed Maven 3.9.10 is an alternative.

1. Initialize a **new, empty** database as described in [sql/README.md](sql/README.md):

   ```sh
   mysql -u root -p < sql/01_schema.sql
   mysql -u root -p < sql/02_constraints.sql
   ```

2. Create your own local database account. In a Bash/Zsh terminal, configure and
   start the backend (the password prompt avoids putting it in shell history):

   ```sh
   export DB_HOST=127.0.0.1 DB_PORT=3306
   export DB_USERNAME=your_local_db_user
   printf 'Database password: '
   read -r -s DB_PASSWORD
   printf '\n'
   export DB_PASSWORD
   export APP_JWT_SECRET="$(openssl rand -hex 32)"
   cd backend
   ./mvnw spring-boot:run
   ```

   Backend: `http://localhost:8080`. `backend/.env.example` documents variables;
   Spring Boot does not load this file automatically. Required credentials have
   no fallback. Keep the generated JWT secret private and stable during a session;
   changing it invalidates existing tokens. Local JPA schema update is retained.

3. In another terminal, start the frontend:

   ```sh
   cd frontend
   cp .env.example .env.local
   pnpm install --frozen-lockfile
   pnpm dev
   ```

   Frontend: `http://localhost:5173`. `VITE_*` values are public browser settings;
   never put passwords or signing keys in them. Register a new account through
   the UI; no default account is supplied. Use port 5173 (or another origin already
   allowed in `WebConfig`) for local API integration.

## Testing

```sh
cd backend
./mvnw test
cd ../frontend
pnpm build
node --experimental-strip-types --test tests/deleted-task-cache.test.mjs
```

Current local results: backend 101 passed / 1 skipped; cache tests 6 passed.
The standard frontend build currently fails with 47 pre-existing TypeScript errors;
standalone Vite bundling passes. A limited MySQL 9.6 smoke check also passed.

See [docs/VALIDATION.md](docs/VALIDATION.md) for actual commands, outcomes and
coverage limits. H2 results do not establish MySQL compatibility. The existing
`Lab1ApplicationTests.contextLoads` test is disabled; no full end-to-end verification
or production readiness is claimed.

## Team Contributions

This was a collaborative software engineering course project. My main areas of
work were **Spring Boot backend development and frontend–backend API integration**,
with assistance on selected frontend features and database-related work.

The complete feature set was delivered by the team. This repository does not
attribute every endpoint, algorithm, UI screen or database change to me, and does
not claim individual ownership where contribution records have not been verified.

Team permission to publish and licensing of the team's original code must be
confirmed before this local candidate is made public. No license is assigned to
the team's original work by this cleanup.

## Acknowledgements

The frontend is based on **[shadcn-vue-admin by Whbbit1999](https://github.com/Whbbit1999/shadcn-vue-admin)**.
The supplied MIT notice is preserved in [frontend/LICENSE](frontend/LICENSE).
Template UI and demo content are not original personal contributions.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for template and icon
attribution. Maven Wrapper scripts retain their Apache license headers.
