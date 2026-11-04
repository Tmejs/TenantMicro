# TenantMicro Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Use the user's one-branch-per-checkpoint workflow; do not create Git worktrees or subagent implementation branches.

**Goal:** Deliver a learning-focused Spring REST service whose JWT-authenticated requests safely create and read tenant-owned projects and issues in shared PostgreSQL tables.

**Architecture:** Keycloak authenticates users and issues JWTs; the service validates tokens and stores tenant memberships itself. Spring Data `JpaRepository` serves both global catalog entities and tenant-owned entities. A short-lived, non-root platform scope authorizes membership and closes its JPA transaction before a verified tenant scope begins; Hibernate `@TenantId` scopes project/issue ORM access, and database keys prevent cross-tenant issue/project links.

**Tech Stack:** Java 21, Spring Boot 4.1.2, Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17.6, Keycloak 26.8.1, JUnit 5, Testcontainers, Maven.

**Spec:** `docs/superpowers/specs/2026-11-03-tenantmicro-design.md`

**Test-harness amendment (2026-11-03):** At the user's request, checkpoint 6 converts the integration tests to `MockMvc` with a full Spring context and real PostgreSQL/Keycloak Testcontainers. This replaces the random-port HTTP client in the remaining plan. The issue/board task moves to checkpoint 7; its test assertions and isolation goals stay the same.

## Global Constraints

- Do not use Git worktrees. Each numbered implementation task below is a separate checkpoint branch created from clean synchronized `main`; merge and verify it before starting the next.
- Keep the root README limited to behavior actually verified by the completed checkpoint.
- No GitHub Actions gate; run focused tests, `./mvnw verify`, and `git diff --check` locally before push.
- Obtain independent review before each push; fix and re-review Critical/Important findings. Record review and verification in `codex/checkpoints/`.
- Use `MockMvc` integration tests with Testcontainers PostgreSQL and Keycloak as the primary test layer. Keep unit tests scarce and purposeful.
- Use `JpaRepository` for all four entities. Annotate only tenant-owned entities with `@TenantId`, never accept or assign their tenant ID from HTTP bodies, and do not use native/bulk SQL for tenant-owned runtime paths.
- No code for epics, sprints, frontend, project-level roles, async jobs, or tenant deletion in this plan.

## Review Focus

Each condition below gets a named test in its owning task; the context cleanup case uses a focused same-thread unit test.

1. A valid JWT with the wrong audience must receive `401` (`JwtSecurityIT` in Task 1).
2. A revoked membership must deny the next request without a cached grant (`TenantMembershipIT` in Task 2).
3. An issue request using a known foreign project UUID must return `404` and create nothing (`IssueIsolationIT` in Task 4).
4. An exception during a tenant request must clear context on the same thread (`TenantRequestFilterTest` in Task 2).
5. A non-admin JWT must receive `403` from every admin mutation endpoint (`AdminTenantIT` in Task 2).
6. A platform-scoped JPA session must not see tenant-owned rows (`ProjectIsolationIT` in Task 3).

## File map

The paths below are the intended small units; keep request DTOs and response DTOs as separate records within their feature package.

- `pom.xml`, `.mvn/wrapper/*`, `mvnw`, `mvnw.cmd`: reproducible Maven build and Surefire/Failsafe integration-test gate.
- `src/main/resources/application.yml`, `db/migration/V1__core.sql`: configuration and authoritative schema; Hibernate validates rather than generates it.
- `src/main/java/com/tmejs/tenantmicro/TenantMicroApplication.java`: Boot entry point.
- `security/SecurityConfig.java`, `security/RealmRoleConverter.java`: JWT validation, audience, role mapping, admin authorization.
- `security/TenantRequestFilter.java`, `security/VerifiedTenantContext.java`, `persistence/TenantIdentifierResolver.java`: platform/tenant scope lifecycle, membership check, and Hibernate tenant resolution.
- `admin/Tenant.java`, `admin/Membership.java`, `admin/MembershipId.java`, `admin/TenantRepository.java`, `admin/MembershipRepository.java`, `admin/TenantAdminController.java`, `admin/TenantAdminService.java`, `admin/TenantCatalog.java`: global tenant and membership management through JPA repositories. The repositories are only called through the catalog service.
- `project/Project.java`, `project/ProjectRepository.java`, `project/ProjectService.java`, `project/ProjectController.java`: tenant-owned projects and endpoints.
- `issue/Issue.java`, `issue/IssueRepository.java`, `issue/IssueService.java`, `issue/IssueController.java`, `issue/IssueStatus.java`: tenant-owned issues, status changes, and board endpoint.
- `api/ApiExceptionHandler.java`: consistent problem responses.
- `src/test/java/com/tmejs/tenantmicro/it/IntegrationEnvironment.java`: shared Testcontainers PostgreSQL/Keycloak lifecycle, real token acquisition, and `MockMvc` request helpers.
- `src/test/java/com/tmejs/tenantmicro/it/*IT.java`: Spring MVC scenario tests.
- `src/test/java/com/tmejs/tenantmicro/security/TenantRequestFilterTest.java`: focused same-thread exception cleanup test; this is the one small unit test justified by the context-lifecycle risk.
- `dev/realm.json`, `compose.yaml`: local Keycloak/PostgreSQL and test identity configuration.
- `codex/checkpoints/checkpoint-<number>.md`: evidence for each implementation checkpoint.

## Checkpoint routine for every task

Tasks 1–3 use branches `codex/checkpoint-<N+2>-<short-name>`; checkpoint 1 is the initial documentation plan and checkpoint 2 updates it for JPA repositories. Checkpoint 6 converts the test harness to MockMvc; Task 4 uses checkpoint 7. Start each task with the first two steps below, then do its focused implementation steps, then finish the remaining steps in order:

- [ ] Confirm `main` and the working tree are clean; `git fetch origin`; inspect unexpected remote commits; confirm `git rev-parse main` equals `git rev-parse origin/main` and `git ls-remote --heads origin main`.
- [ ] Create the named branch from synchronized `main` in the same checkout, with `git switch -c ...`.
- [ ] Keep implementation commits focused. Add a separate focused review-fix commit if needed.
- [ ] Run the task's focused `*IT`, `./mvnw verify`, and `git diff --check`; write commands, results, review findings, and exact commit IDs to the task's `codex/checkpoints/checkpoint-<number>.md`.
- [ ] Request independent review of the branch, fix all Critical/Important findings, and re-review fixes. Commit the final evidence record.
- [ ] Push the branch, then confirm `HEAD`, tracking branch, and `git ls-remote` branch SHA match. Preserve the branch.
- [ ] Recheck clean synchronized `main`, merge the branch into `main` with `git merge --no-ff`, run `./mvnw verify` and `git diff --check` on merged `main`, push `main`, and confirm local/tracking/remote SHAs match before the next task.

---

### Task 1: Boot, JWT, and real integration-test harness (checkpoint 3)

**Files:** Create build/wrapper files, application entry point/configuration, security files, `api/ApiExceptionHandler.java`, `dev/realm.json`, `compose.yaml`, `IntegrationEnvironment.java`, `JwtSecurityIT.java`, and `codex/checkpoints/checkpoint-3.md`; update README with verified start/test instructions.

**Interfaces:** `RealmRoleConverter` maps Keycloak `realm_access.roles` to `ROLE_*`; `SecurityConfig` validates issuer, audience `tenantmicro-api`, signature, expiry, and secures `/api/admin/**` for `ROLE_PLATFORM_ADMIN`. `IntegrationEnvironment` supplies `adminToken()`, `userToken(String username)`, and `MockMvc` request helpers after checkpoint 6. No tenant APIs yet; a simple authenticated `/api/whoami` endpoint proves the JWT flow and returns `sub`.

- [ ] Add the Maven wrapper and minimal `pom.xml` with Boot 4.1.2, Java 21, JUnit, Failsafe `*IT` binding, and Testcontainers dependencies; confirm `./mvnw -version` runs.
- [ ] Write `JwtSecurityIT` using Testcontainers PostgreSQL and Keycloak. Assert valid user token returns `200` and its subject from `/api/whoami`; missing, expired/invalid-signature, and wrong-audience tokens return `401`; non-admin token at `/api/admin/tenants` returns `403`.
- [ ] Run `./mvnw -Dit.test=JwtSecurityIT verify`; expect failure because application/security endpoints do not exist.
- [ ] Add the Spring Boot entry point, local realm/test users, containers, JWT configuration, role conversion, and `/api/whoami` with the smallest code that satisfies the test. Do not enable password grant on any production client; direct access grants are test-only.
- [ ] Re-run focused IT and `./mvnw verify`; expect all tests to pass. Complete checkpoint routine and merge before Task 2.

### Task 2: Platform admin, membership, and tenant request boundary (checkpoint 4)

**Files:** Create `V1__core.sql`, global `Tenant`/`Membership` entities and `JpaRepository` interfaces, `TenantAdminController.java`, `TenantAdminService.java`, `TenantCatalog.java`, `TenantRequestFilter.java`, `VerifiedTenantContext.java`, `TenantIdentifierResolver.java`, `AdminTenantIT.java`, `TenantMembershipIT.java`, `TenantRequestFilterTest.java`, and `codex/checkpoints/checkpoint-4.md`; update application configuration and README.

**Interfaces:** `TenantRepository extends JpaRepository<Tenant,String>` and `MembershipRepository extends JpaRepository<Membership,MembershipId>` have no `@TenantId`; `MembershipId` is `(tenantId,subject)`. `TenantCatalog` uses those repositories for `createTenant(id,name)`, `listTenants()`, `tenantExists(id)`, `grant(tenantId,subject)`, `revoke(tenantId,subject)`, `isMember(tenantId,subject)` within service transactions. `VerifiedTenantContext.requireTenantId()` returns a verified slug or throws in platform/absent scope; `currentHibernateIdentifier()` returns the tenant slug or reserved `__platform__` and throws when absent. `TenantIdentifierResolver` delegates to the latter and never reports `__platform__` as a root tenant. `TenantRequestFilter` runs after JWT authentication: admin paths get platform scope; tenant paths get platform scope for a membership transaction, close it, then get verified tenant scope for the downstream chain. Each scope clears in `finally`; switching while a JPA transaction/session is open is forbidden. Set `spring.jpa.open-in-view=false`, and reject `__platform__` as a URL tenant slug.

- [ ] Write `AdminTenantIT`: platform admin creates/lists tenants, grants/revokes membership; duplicate tenant returns `409`; unknown tenant `404`; invalid slug/body `400`; regular users receive `403` on admin mutations.
- [ ] Write `TenantMembershipIT`: an admin grants a membership; member can reach a temporary tenant echo endpoint; non-member receives `403`; revoked member receives `403` on the next request; unknown tenant receives `404`.
- [ ] Write `TenantRequestFilterTest`: on one test thread, invoke the filter with an authenticated member and a downstream chain that throws; assert `VerifiedTenantContext.requireTenantId()` fails immediately after the filter returns by exception. Also assert it fails before any tenant request and that membership JPA work finishes in platform scope before the tenant chain begins. This focused test may invoke the filter directly with mock request/response objects.
- [ ] Run `./mvnw -Dit.test=AdminTenantIT,TenantMembershipIT verify`; expect failures for missing schema and endpoints; run `./mvnw -Dtest=TenantRequestFilterTest test` and expect failure before the filter exists.
- [ ] Add Flyway schema for global tables and the future project/issue tables, global JPA entities/repositories/catalog, admin endpoints, scope filter/context/resolver, and error handler mappings. Configure filter ordering and service transaction boundaries so membership JPA work closes before the tenant JPA session opens.
- [ ] Re-run focused IT and `./mvnw verify`; expect all tests to pass. Complete checkpoint routine and merge before Task 3.

### Task 3: Tenant-scoped projects (checkpoint 5)

**Files:** Create `Project.java`, `ProjectRepository.java`, `ProjectService.java`, `ProjectController.java`, `ProjectIsolationIT.java`, and `codex/checkpoints/checkpoint-5.md`; update README.

**Interfaces:** `Project` has UUID `id`, `@TenantId String tenantId`, and nonblank `name`; Hibernate populates `tenantId` on persist. `ProjectRepository extends JpaRepository<Project,UUID>` uses normal `save`, `findById`, and `findAll` under a verified tenant session. `ProjectService.create(String name)`, `list()`, and `get(UUID id)` are transactional; neither repository methods nor service methods accept tenant ID from request bodies. HTTP routes: `POST /api/tenants/{tenantId}/projects`, `GET /api/tenants/{tenantId}/projects`, and `GET /api/tenants/{tenantId}/projects/{projectId}`.

- [ ] Write `ProjectIsolationIT`: create two tenants and memberships through admin HTTP, acquire different real user JWTs, create projects in both, then assert each list shows only its tenant, a known foreign UUID returns `404`, a non-member receives `403`, and blank name returns `400`. Also assert platform-scoped repository `findAll()` and `findById(knownProjectId)` see no tenant project, proving the reserved scope is not root.
- [ ] Run `./mvnw -Dit.test=ProjectIsolationIT verify`; expect failures for missing project routes.
- [ ] Implement project entity/repository/service/controller, ensuring JPA sessions start only after tenant verification and UUID lookups remain tenant-scoped. Return `201` for create and `404` for an absent/foreign project.
- [ ] Re-run focused IT and `./mvnw verify`; expect all tests to pass. Complete checkpoint routine and merge before Task 4.

### Task 4: Tenant-scoped issues and board (checkpoint 7)

**Files:** Create `Issue.java`, `IssueRepository.java`, `IssueService.java`, `IssueController.java`, `IssueStatus.java`, `IssueIsolationIT.java`, `BoardIT.java`, and `codex/checkpoints/checkpoint-7.md`; update README.

**Interfaces:** `Issue` has UUID `id`, `@TenantId String tenantId`, UUID `projectId`, nonblank `title`, and `IssueStatus status`; Hibernate populates `tenantId` on persist and creation defaults to `TODO`. `IssueRepository extends JpaRepository<Issue,UUID>` adds `findByProjectId(UUID projectId)` for the board/list under the verified tenant session. `IssueService.create(UUID projectId,String title)`, `list(UUID projectId)`, `get(UUID projectId,UUID issueId)`, `changeStatus(UUID projectId,UUID issueId,IssueStatus status)`, and `board(UUID projectId)` check that the project is visible to the current tenant. Board JSON contains exactly `TODO`, `IN_PROGRESS`, and `DONE` arrays.

- [ ] Write `IssueIsolationIT`: create two tenants and projects through `MockMvc`; give a user membership in both tenants, then use a valid tenant URL with a known foreign project/issue UUID and assert `POST`/`GET`/`PATCH` return `404` without mutation. A user who is not a member of the URL tenant receives `403` before resource lookup. Invalid status/title returns `400`; a direct test-only SQL insert confirms the composite foreign key rejects a cross-tenant issue/project pair.
- [ ] Write `BoardIT`: create three issues, move one to `IN_PROGRESS` and one to `DONE`, and assert the board has exactly one issue in each column and does not include the other tenant's issues.
- [ ] Run `./mvnw -Dit.test=IssueIsolationIT,BoardIT verify`; expect failures for missing issue routes.
- [ ] Implement issue endpoints, default/status transitions, board grouping, and tenant-safe lookups. Preserve the composite foreign key in the migration; no new table migration should be needed if Task 2 created the schema correctly.
- [ ] Re-run focused IT and `./mvnw verify`; expect all tests to pass. Complete checkpoint routine and merge before declaring the first version complete.

## Final audit

- [ ] Before checkpoint 7's final evidence commit, run a local smoke test against Compose Keycloak/PostgreSQL and record the result. After the merge, run `./mvnw verify` and `git diff --check` on `main`.
- [ ] Confirm README matches verified endpoints and setup; confirm no test credentials are used in production configuration.
- [ ] Confirm `main`, `origin/main`, and remote `main` SHAs match, and all checkpoint branches remain available.
