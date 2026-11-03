# Checkpoint 4: Tenant management and request boundary

Branch: `codex/checkpoint-4-tenant-boundary`

## Scope

- Flyway schema for global tenant/membership catalog and future tenant-owned project/issue tables, including a composite issue/project foreign key.
- Spring Data `JpaRepository` for global tenant and membership entities, called only through a platform-scoped catalog.
- Platform-admin create/list/grant/revoke endpoints and consistent JSON errors for domain/validation failures.
- JWT-authenticated tenant request filter that checks membership in a short-lived platform scope before opening a verified tenant scope; Hibernate resolver fails without scope and never treats platform scope as root.
- Ten HTTP integration tests across JWT, admin, and membership flows, plus one focused same-thread cleanup test.

## Verification and review

- Started from clean synchronized `main` at `aecf1cae73e3c22ed8ce9817b872c53b6220c5d7` after fetch and remote inspection.
- Red phase: new admin and membership HTTP tests returned 404 for missing endpoints before implementation.
- Focused `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw -q -Dit.test=AdminTenantIT,TenantMembershipIT verify`: pass.
- Focused `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw -q -Dtest=TenantRequestFilterTest test`: pass.
- Full `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw verify`: pass, eleven tests total, zero failures/errors/skips (2026-11-03 10:19 Europe/Warsaw).
- `git diff --check` and staged `git diff --cached --check`: pass.
- Independent review found two Important gaps: an overlong tenant name produced a server error, and the scope unit test could not prove real JPA handoff. `ad4848e` adds the length validation and a test-only HTTP transaction/EntityManager probe after real catalog membership calls. Re-review found no remaining Critical or Important findings.

## Commits

- Implementation: `d0b928b`.
- Focused review fixes: `ad4848e`.
