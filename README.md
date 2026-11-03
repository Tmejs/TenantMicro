# TenantMicro

TenantMicro is a learning project for a shared-table multitenant Spring web service. The current checkpoint provides a Spring Boot application, Keycloak JWT authentication, and a real HTTP integration-test harness with PostgreSQL and Keycloak Testcontainers. Tenant management and tenant-owned data APIs are planned next.

## Prerequisites

Java 21 and a running Docker daemon. The Maven wrapper downloads Maven 3.9.6 on first use.

## Run the tests

```sh
./mvnw verify
```

The `JwtSecurityIT` integration tests start PostgreSQL and Keycloak containers, acquire real access tokens, and check issuer, audience, signature, and admin-route access over HTTP.

## Run locally

```sh
docker compose up -d
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tenantmicro \
SPRING_DATASOURCE_USERNAME=tenantmicro \
SPRING_DATASOURCE_PASSWORD=tenantmicro-dev \
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8080/realms/tenantmicro \
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Keycloak's local admin console is at `http://localhost:8080` with `admin` / `admin-dev`. The `tenantmicro` realm includes a `tenantmicro-local` browser client that adds the `tenantmicro-api` audience. Create realm users in the console and assign `PLATFORM_ADMIN` only to platform administrators. Direct access grants are enabled only in the integration-test realm.

The current API has one endpoint: `GET /api/whoami` returns the authenticated JWT subject. All API requests require a bearer token. `/api/admin/**` requires `PLATFORM_ADMIN`; admin operations will be added in the next checkpoint.

The [design](docs/superpowers/specs/2026-11-03-tenantmicro-design.md) and [implementation plan](docs/superpowers/plans/2026-11-03-tenantmicro-implementation.md) describe the upcoming tenant boundaries and Jira-style features.
