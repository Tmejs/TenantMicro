# Checkpoint 1: design and implementation plan

Branch: `codex/checkpoint-1-implementation-plan`

## Scope

- Captured the approved first-version multitenant service design.
- Wrote four sequential implementation checkpoints, each with an integration-test gate and branch/merge verification routine.
- No application code was added.

## Verification and review

- Repository bootstrap: remote had no branches; a README-only root commit established `main`. Local, tracking, and remote `main` all matched at `12977e059c04e30d7fb6139e501812f2381b21e8` before this branch was created.
- Builder check of the design: no important unanswered questions.
- Independent plan review found two Important test-plan issues: an HTTP follow-up did not prove same-thread tenant-context cleanup, and foreign tenant access conflated `403` membership failure with `404` foreign-resource lookup. Both were corrected and independently re-reviewed with no Critical or Important findings remaining.
- `git diff --check` and staged diff whitespace check: pass.
- Maven tests: inapplicable; this checkpoint changes documentation only and the project has no build yet.

## Sources checked for version and framework decisions

- Spring Boot 4.1.2 is a published stable release: https://spring.io/blog/category/releases/
- Keycloak 26.8.1 is a published release: https://www.keycloak.org/downloads
- Hibernate multitenancy model and `@TenantId`: https://docs.hibernate.org/orm/current/userguide/html_single/#multitenancy
- Spring Security resource-server multitenancy: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/multitenancy.html
