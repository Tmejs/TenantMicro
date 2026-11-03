# Checkpoint 2: JPA repository plan amendment

Branch: `codex/checkpoint-2-jpa-repository-plan`

## Scope

- Use Spring Data `JpaRepository` for global `Tenant` and `Membership` plus tenant-owned `Project` and `Issue`.
- Keep Hibernate `@TenantId` only on tenant-owned entities; establish a non-root platform scope for short-lived global JPA transactions before switching to a verified tenant scope.
- Renumber implementation checkpoints to 3 through 6. No application code was added.

## Verification and review

- Started from clean, fetched `main`; local, tracking, and remote `main` matched at `7625f3108c95994802167685dd0f9ddbb519c8e7`.
- Independent review found one Important contradiction: the admin request flow still said it used no scope. The design now explicitly enters platform scope. Review also asked for a known-ID platform isolation assertion; the plan now includes it.
- Re-review reported no remaining Critical or Important findings.
- Staged diff whitespace check: pass.
- Maven tests: inapplicable; this checkpoint changes documentation only.

## Reference

- Hibernate `@TenantId` is set from the current tenant identifier: https://docs.hibernate.org/orm/7.2/javadocs/org/hibernate/annotations/TenantId.html
- Spring Data JPA repository methods and service transactions: https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html
