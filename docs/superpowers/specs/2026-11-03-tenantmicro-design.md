# TenantMicro first-version design

## Intent

Build a learning-focused Java Spring REST microservice that demonstrates the complete HTTP-to-database flow of a shared-table multitenant application. It should make tenant authorization and isolation visible in code and prove the boundary through real HTTP integration tests.

## Scope

- A platform administrator creates tenants and grants or revokes user membership.
- A tenant member creates projects and issues, lists and reads them, moves issues among `TODO`, `IN_PROGRESS`, and `DONE`, and views a three-column board for one project.
- Keycloak issues JWTs. The service validates the issuer, signature, expiry, and audience. A Keycloak realm role `PLATFORM_ADMIN` protects global administration endpoints. Tenant membership is stored in the service database and checked on every tenant-scoped request.
- Tenant-scoped URLs begin with `/api/tenants/{tenantId}`. The path identifies the requested tenant; it never authorizes access by itself.
- PostgreSQL uses shared tables. Every project and issue row has a `tenant_id`. Cross-tenant project/issue relationships are rejected by database constraints as well as application checks.
- Main paths and isolation failures are covered by HTTP integration tests using Testcontainers for PostgreSQL and Keycloak. Tests provision tenants and memberships through admin endpoints, obtain real JWTs, and use the public API. Unit tests are limited to logic that cannot be tested clearly through integration tests.

## Boundaries and data model

`tenants(id, name)` uses a stable URL-safe slug as `id`. `memberships(tenant_id, subject)` stores the Keycloak JWT `sub` claim; neither table is tenant-partitioned. Tenant creation and membership mutation are global administrator operations.

`projects(id, tenant_id, name)` and `issues(id, tenant_id, project_id, title, status)` are tenant-partitioned. IDs are UUIDs. A unique `(tenant_id, id)` key on projects and a composite foreign key from `(issues.tenant_id, issues.project_id)` to `(projects.tenant_id, projects.id)` prevent an issue from referencing another tenant's project. Issue status is one of the three board columns. The first version has no epics, sprints, comments, attachments, or project-specific roles.

The application uses Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA/Hibernate, Flyway migrations, and PostgreSQL. Use `JpaRepository` for `Tenant`, `Membership`, `Project`, and `Issue`. Only `Project` and `Issue` carry Hibernate `@TenantId`; Hibernate populates that field from the session tenant identifier, so request bodies and service methods must not assign it. Tenant-owned repositories are called only after membership verification and within tenant-scoped service transactions. Native SQL or bulk operations against tenant-owned tables require separate scrutiny and explicit tenant predicates.

Global catalog repositories run in a reserved platform scope that is not a root tenant and is never accepted as a URL tenant slug. Before authorizing a tenant request, a short-lived platform-scoped transaction reads `Tenant` and `Membership` through JPA, closes, and only then the request enters its verified tenant scope. Admin requests use the same platform scope for their transaction. A `CurrentTenantIdentifierResolver` reads the explicit current scope and fails when absent. Disable Open EntityManager in View; never switch scopes while a JPA session or transaction is open. The platform scope must never grant access to tenant-owned entities.

## Request flow

1. Spring Security validates the bearer JWT. Invalid or missing credentials produce `401`.
2. For `/api/admin/**`, Spring Security requires `PLATFORM_ADMIN`. These endpoints enter the reserved platform scope and use the global JPA repositories; they do not enter a customer tenant scope.
3. For `/api/tenants/{tenantId}/**`, validate the slug, use a short-lived platform-scoped JPA transaction to look up the tenant and `(tenantId, sub)` membership, close that transaction, and reject missing membership with `403`.
4. Set the verified tenant context before entering a transactional tenant service. The Hibernate session uses that tenant and `@TenantId` scopes mapped entities. Disable Open EntityManager in View so the session cannot open before tenant verification. Clear context in `finally` on success and failure. No asynchronous request processing is in the first version.
5. Return tenant-owned data. A valid member requesting a record that belongs to another tenant receives `404`, including when the UUID is known.

## HTTP contract

- `POST /api/admin/tenants` with `{ "id": "acme", "name": "Acme" }`: create tenant, `201`; duplicate ID, `409`.
- `GET /api/admin/tenants`: list tenants.
- `PUT /api/admin/tenants/{tenantId}/members/{subject}`: grant membership idempotently; unknown tenant, `404`.
- `DELETE /api/admin/tenants/{tenantId}/members/{subject}`: revoke membership idempotently; unknown tenant, `404`.
- `POST /api/tenants/{tenantId}/projects` with `{ "name": "Delivery" }`: create project, `201`.
- `GET /api/tenants/{tenantId}/projects` and `GET /api/tenants/{tenantId}/projects/{projectId}`: list/read projects.
- `POST /api/tenants/{tenantId}/projects/{projectId}/issues` with `{ "title": "First task" }`: create issue in `TODO`, `201`.
- `GET /api/tenants/{tenantId}/projects/{projectId}/issues` and `GET /api/tenants/{tenantId}/projects/{projectId}/issues/{issueId}`: list/read issues.
- `PATCH /api/tenants/{tenantId}/projects/{projectId}/issues/{issueId}/status` with `{ "status": "IN_PROGRESS" }`: change status.
- `GET /api/tenants/{tenantId}/projects/{projectId}/board`: return three status columns with the project's issues.

All JSON payloads reject blank names/titles and malformed IDs with `400`. A missing tenant or tenant-owned resource returns `404` after authentication and authorization. Use a consistent problem response body with an error code and message. No endpoint creates Keycloak users; the platform admin supplies an existing Keycloak `sub` when granting membership.

## Local development and testing

Pin Spring Boot to 4.1.2, Java to 21, Keycloak container to 26.8.1, and PostgreSQL container to 17.6 in the initial implementation. Keep credentials and test users in local development/test configuration only. Provide a local Keycloak realm import with the platform role, audience, and sample users, plus a Docker Compose setup for Keycloak and PostgreSQL.

Tests use Testcontainers for both PostgreSQL and Keycloak and real HTTP calls to a random-port Spring Boot instance. A test-only Keycloak client may use direct access grants to acquire user JWTs; that grant must not be enabled for a production client. Test fixtures create tenants and memberships via admin endpoints, not direct inserts. The full local gate is `./mvnw verify`, including integration tests, with Docker available; no GitHub Actions gate is required.

Critical isolation scenarios: two tenant users see only their own projects/issues; known foreign UUIDs cannot be read or changed; missing/invalid JWT and absent membership are rejected; membership revocation takes effect on the next request; project/issue constraints reject cross-tenant references; context does not leak across sequential requests or exceptions; platform scope cannot read tenant-owned rows or become a root tenant; admin endpoints reject non-admin tokens.

## Delivery workflow

Use the user's Git checkpoint flow. Do not use Git worktrees. Each implementation checkpoint starts from clean, fetched, synchronized `main` on a fresh `codex/checkpoint-<number>-<name>` branch. Run focused checks, the full local gate, `git diff --check`, and an independent review; record verification/review evidence under `codex/` if that convention is established. Push only after review passes, confirm branch SHAs, merge into `main` with `--no-ff`, verify the merge, push `main`, and confirm local/tracking/remote SHAs before starting the next checkpoint. Preserve checkpoint branches.

## Deliberate limits

This is one service, not a distributed suite. The first version has no frontend, self-service tenant signup, tenant deletion, custom domains, per-project permissions, epics, sprints, caching, background jobs, or cross-tenant reporting. PostgreSQL row-level security is a possible later defense-in-depth exercise, not part of this first version.
