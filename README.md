# TenantMicro

TenantMicro is a learning project for a shared-table multitenant Spring web service. It provides Keycloak JWT authentication, platform-admin tenant and membership management, verified tenant request boundaries, tenant-owned projects and issues, and a three-column project board.

## Prerequisites

Java 21 and a running Docker daemon. The Maven wrapper downloads Maven 3.9.6 on first use.

## Run the tests

```sh
./mvnw verify
```

The integration tests start PostgreSQL and Keycloak containers, acquire real access tokens, and run requests through Spring MVC and Security with `MockMvc`. They cover JWT validation, tenant membership, and project isolation without opening a local application port.

## Run locally

```sh
docker compose up -d
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tenantmicro \
SPRING_DATASOURCE_USERNAME=tenantmicro \
SPRING_DATASOURCE_PASSWORD=tenantmicro-dev \
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8080/realms/tenantmicro \
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Keycloak's local admin console is at `http://localhost:8080` with `admin` / `admin-dev`. The `tenantmicro` realm includes a `tenantmicro-local` browser client that adds the `tenantmicro-api` audience and sample users `platform-admin` / `platform-admin-dev` and `alice` / `alice-dev`. The platform user has `PLATFORM_ADMIN`. Direct access grants are enabled only in the integration-test realm.

`GET /api/whoami` returns the authenticated JWT subject. All API requests require a bearer token. `/api/admin/**` requires `PLATFORM_ADMIN`:

- `POST /api/admin/tenants` with `{ "id": "acme", "name": "Acme" }` creates a tenant.
- `GET /api/admin/tenants` lists tenants.
- `PUT /api/admin/tenants/{tenantId}/members/{subject}` grants membership idempotently.
- `DELETE /api/admin/tenants/{tenantId}/members/{subject}` revokes membership idempotently.

`GET /api/tenants/{tenantId}/context` returns the verified tenant ID and JWT subject for an authorized member. Membership is checked against PostgreSQL on every tenant request; a revoked member is denied on the next request.

Tenant members can create and read projects:

- `POST /api/tenants/{tenantId}/projects` with `{ "name": "Delivery" }` creates a project.
- `GET /api/tenants/{tenantId}/projects` lists that tenant's projects.
- `GET /api/tenants/{tenantId}/projects/{projectId}` reads one project. A known project ID from another tenant returns `404`.

The project repository is a normal Spring Data `JpaRepository`. Hibernate `@TenantId` fills and filters `tenant_id` from the verified request scope; the API never accepts it from JSON.

Tenant members can manage issues within a visible project:

- `POST /api/tenants/{tenantId}/projects/{projectId}/issues` with `{ "title": "First task" }` creates an issue in `TODO`.
- `GET /api/tenants/{tenantId}/projects/{projectId}/issues` lists the project's issues; append `/{issueId}` to read one.
- `PATCH /api/tenants/{tenantId}/projects/{projectId}/issues/{issueId}/status` with `{ "status": "IN_PROGRESS" }` moves an issue among `TODO`, `IN_PROGRESS`, and `DONE`.
- `GET /api/tenants/{tenantId}/projects/{projectId}/board` groups the project's issues into those three columns.

The issue repository also uses `JpaRepository` and `@TenantId`. The database has a composite foreign key from `(issues.tenant_id, issues.project_id)` to `(projects.tenant_id, projects.id)`; a cross-tenant link is rejected even if application code misses a check.

The [design](docs/superpowers/specs/2026-11-03-tenantmicro-design.md) and [implementation plan](docs/superpowers/plans/2026-11-03-tenantmicro-implementation.md) describe the upcoming tenant boundaries and Jira-style features.
