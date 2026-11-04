# Checkpoint 6: MockMvc integration-test harness

Branch: `codex/checkpoint-6-mockmvc-tests`

## Scope

- Replace the random-port application HTTP client with `MockMvc` in the shared integration-test environment.
- Keep real PostgreSQL and Keycloak Testcontainers and real Keycloak bearer tokens. `MockMvc` exercises the Spring MVC and Security filter chain in a full application context.
- Guard against a missing JWT audience claim; a wrong-audience token now returns `401` cleanly in-process.
- Update design, plan, and README to reflect the requested harness and move issues/board to checkpoint 7.

## Verification and review

- Started from clean synchronized `main` at `9215c5f268e9d9449fc9d331cc2559e60583524c` after fetch and remote inspection.
- First full run surfaced a null-audience validator exception under MockMvc; fixed in the implementation commit.
- `JAVA_HOME=/Users/matrzad/.sdkman/candidates/java/21.0.8-tem ./mvnw verify`: pass, twelve tests total, zero failures/errors/skips (2026-11-03 Europe/Warsaw).
- `git diff --check main...HEAD` and staged `git diff --cached --check`: pass.
- Independent review found no Critical or Important findings. Minor documentation contradictions were fixed in `989b9f2` and the final evidence commit; follow-up review found no Critical or Important findings.

## Commits

- Harness and validator: `4b5418b`.
- Documentation review fix: `989b9f2`.
