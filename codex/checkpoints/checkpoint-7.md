# Checkpoint 7: Tenant-scoped issues and board

Branch: `codex/checkpoint-7-issues-board`

## Scope

- `Issue` uses Hibernate `@TenantId`; `IssueRepository` is a normal Spring Data `JpaRepository` with `findByProjectId`.
- Create/list/read/status and three-column board endpoints verify project visibility before issue repository access.
- Hibernate fills issue tenant IDs; request DTOs cannot set them. The composite database foreign key rejects cross-tenant project links.
- The derived issue query is initialized lazily because eager repository validation tries to resolve a tenant before a request scope exists. The resolver still fails without a scope at runtime.

## Verification and review

- Started from clean synchronized `main` at `493f98d4d86ebabcef05cd41c86fe81e00435549` after fetch and remote inspection.
- Red phase: `IssueIsolationIT` and `BoardIT` failed with 404 at missing issue routes.
- Focused `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw -q -Dit.test=IssueIsolationIT,BoardIT verify`: pass after implementation.
- Focused `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw -q -Dit.test=IssueIsolationIT verify`: pass after review-fix assertions.
- Full `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw verify`: pass, 13 integration tests plus one unit test, zero failures/errors/skips (2026-11-04 Europe/Warsaw).
- `git diff --check main...HEAD` and staged diff checks: pass.
- Independent review found no Critical or Important findings. It suggested a same-tenant different-project issue test; `17a6c59` added GET/PATCH 404 plus unchanged status assertions. Re-review found no remaining Critical or Important findings.
- Compose smoke: `docker compose config --quiet` and `docker compose up -d` passed; Keycloak realm discovery returned 200; the app started against Compose PostgreSQL/Keycloak, Flyway applied V1, all four core tables existed, and unauthenticated `GET /api/whoami` returned 401. The temporary app and containers were stopped.
- Credentials and direct access grants occur only in local development/test files; `src/main` has none. The local browser client has direct access grants disabled.

## Commits

- Implementation: `64c281f`.
- Review coverage fix: `17a6c59`.
