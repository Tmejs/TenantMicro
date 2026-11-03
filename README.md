# TenantMicro

TenantMicro is a learning project for a shared-table multitenant Spring web service. It currently provides Keycloak JWT authentication, platform-admin tenant and membership management, and a verified tenant request boundary. Project and issue APIs are planned next.

## Prerequisites

Java 21 and a running Docker daemon. The Maven wrapper downloads Maven 3.9.6 on first use.

## Run the tests

```sh
./mvnw verify
```

The `JwtSecurityIT` integration tests start PostgreSQL and Keycloak containers, acquire real access tokens, and check subject, audience, signature, expiry, and admin-route access over HTTP.

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

The [design](docs/superpowers/specs/2026-11-03-tenantmicro-design.md) and [implementation plan](docs/superpowers/plans/2026-11-03-tenantmicro-implementation.md) describe the upcoming tenant boundaries and Jira-style features.
