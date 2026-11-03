# Checkpoint 5: Tenant-scoped projects

Branch: `codex/checkpoint-5-project-isolation`

## Scope

- `Project` entity with Hibernate `@TenantId`, normal Spring Data `JpaRepository`, service transactions, and create/list/read HTTP API.
- Tenant ownership comes from verified request scope; project request bodies contain no tenant ID.
- HTTP isolation test covers two tenants, member/non-member access, known foreign UUID, and invalid project name.
- Repository test confirms platform scope cannot see tenant projects by `findAll` or known `findById`, and absent scope fails closed.

## Verification and review

- Started from clean synchronized `main` at `db82a69a6caaf17110b1daa100faf0ecc8f24a3d` after fetch and remote inspection.
- Red phase: project HTTP test failed with 404 for the missing create route.
- Focused `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw -q -Dit.test=ProjectIsolationIT verify`: pass.
- Full `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw verify`: pass, twelve tests total, zero failures/errors/skips (2026-11-03 10:30 Europe/Warsaw).
- `git diff --check` and staged `git diff --cached --check`: pass.
- Independent review found no Critical or Important findings. A minor test coverage suggestion for owned reads and invalid UUIDs was added in `f65704e`; the focused and full gates passed after that change.

## Commits

- Implementation: `3d795c1`.
- Test coverage review fix: `f65704e`.
