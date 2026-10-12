---
description: Phase 0: repo setup, build skeleton, local infra, CI skeleton
---

# LedgerFlow Phase 0: Repo setup

Read `docs/SPEC.md` (sections listed below) and follow `.github/copilot-instructions.md`.
Spec sections to read for this phase: Tech stack, Services, Local developer experience, CI/CD, Build phases (Phase 0).

Work through the steps below ONE AT A TIME. For each step:
1. Explain the design in a few sentences (why, not just what).
2. Generate the code for that step only.
3. Write its tests (unit and, where the step touches DB/Kafka/Redis/Keycloak, a Testcontainers integration test).
4. Give me the exact commands to run (build, test, format) and a Conventional Commit message.
5. STOP and wait for me to say "next" before moving on.

If you are not sure of a library or image version, tell me to check the latest stable version instead of guessing.

## Steps
1. Parent POM (Java 21, Spring Boot 3.5.x parent, Spring Cloud BOM, dependencyManagement, pluginManagement for Spotless, JaCoCo, Surefire/Failsafe) plus the Maven wrapper.
2. Empty modules for every service in the spec (gateway-service, config-server, account-service, payment-service, ledger-service, fraud-service, notification-service, copilot-service) plus a `common` module for shared event records and the `Money` value object. Each module gets a minimal `@SpringBootApplication` and a context-loads test.
3. `.gitignore`, `.editorconfig`, `.env.example` (no real secrets), Spotless with google-java-format wired into `verify`.
4. `docker-compose.yml` with Postgres 16, Kafka (KRaft, no ZooKeeper) and Redis 7, with healthchecks; a Makefile with `up`, `down`, `test`, `lint`.
5. ArchUnit test module/rules: no `double`/`float` field named `amount`; controllers must not depend on repositories.
6. GitHub Actions `ci.yml` skeleton: checkout, JDK 21, Maven cache, Spotless check, `./mvnw verify`, JaCoCo report upload. Add `dependabot.yml` (weekly) and a CodeQL workflow.
7. README skeleton (pitch, architecture placeholder, how to run, testing pyramid placeholder, money-safety table placeholder) and `docs/adr/0001-no-eureka-on-kubernetes.md`.
8. A list of GitHub issues (one per phase, with acceptance criteria) I can paste into a GitHub Projects board for 1-week sprints.

## Done when
- `./mvnw verify` is green with Spotless and ArchUnit active.
- `docker compose up` brings Postgres, Kafka and Redis up healthy.
- CI runs on a PR and passes.
- Code compiles, `./mvnw verify` (and frontend tests if touched) pass locally and in CI.
- README updated for this phase. No secrets committed.
- You give me 3 interview questions about this phase with short model answers.
