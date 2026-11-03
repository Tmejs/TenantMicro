# Checkpoint 3: JWT foundation

Branch: `codex/checkpoint-3-jwt-foundation`

## Scope

- Boot 4.1.2 / Java 21 Maven application with JWT resource-server security and Keycloak realm-role mapping.
- `GET /api/whoami` and platform-admin route authorization.
- PostgreSQL and Keycloak Testcontainers integration harness, with six real HTTP JWT scenarios.
- Local Compose services and verified startup instructions.

## Verification and review

- Started from clean synchronized `main` at `c89a73421eb4beaf706d3ca906e7c98cb728c559` after fetch and remote inspection.
- Red phase: tests failed for missing app/security behavior before implementation.
- Focused `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw -q -Dit.test=JwtSecurityIT verify`: pass, six tests.
- Full `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw verify`: pass, six tests, zero failures/errors/skips (2026-11-03 10:08 Europe/Warsaw).
- `git diff --check` and staged `git diff --cached --check`: pass.
- `docker compose config --quiet` and `docker compose up -d`: pass. Local Keycloak discovery returned HTTP 200; the application started against Compose PostgreSQL/Keycloak and `GET /api/whoami` without a token returned HTTP 401. Temporary containers were stopped.
- Independent review found two Important test gaps: the subject assertion was not exact and token expiry was untested. Fixed in `14c27bbc491a125ff2000d30e44eb4a8e625181e`; re-review found no remaining Critical or Important findings.

## Commits and limits

- Implementation: `0e1830b`.
- Focused review fixes: `14c27bb`.
- `ApiExceptionHandler` is deferred to checkpoint 4, where domain/API errors first occur.
- The Keycloak direct-access grant clients and short token lifespan are confined to the integration-test realm. The local development client uses browser flow and has direct access grants disabled.
