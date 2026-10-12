# LedgerFlow

> A real-time payments platform built to look and behave like a real bank system: double-entry ledger, real-time fraud scoring, and a human-in-the-loop analyst workflow — with every money-safety guarantee backed by a test.

[![CI](https://github.com/jeranjoel-ai/LedgerFlow/actions/workflows/ci.yml/badge.svg)](https://github.com/jeranjoel-ai/LedgerFlow/actions/workflows/ci.yml)
[![CodeQL](https://github.com/jeranjoel-ai/LedgerFlow/actions/workflows/codeql.yml/badge.svg)](https://github.com/jeranjoel-ai/LedgerFlow/actions/workflows/codeql.yml)
<!-- TODO (Phase 9): coverage badge, uptime badge -->

**Live demo:** _coming in Phase 8_
**Demo credentials:** _coming in Phase 8_ (read-only customer + analyst accounts)
**90-second demo video:** _coming in Phase 9_

---

## What this is

Customers hold accounts, send money to each other, and see their balance update live. Every money
movement is recorded in a double-entry ledger. Every transaction is scored for fraud in real time;
suspicious ones are held for review. A fraud analyst dashboard shows held transactions with an
AI-generated explanation, and a human analyst approves or rejects — the AI copilot never decides
anything on its own.

Full design spec: [`docs/SPEC.md`](docs/SPEC.md).

## Architecture

<!-- TODO (Phase 1-3): replace with a Mermaid diagram once services, gateway, and events exist -->
_Architecture diagram placeholder — services, event flow sequence diagram, and the transfer saga
state machine will be added here as they're built._

Planned services (one Maven module + Docker image each): `gateway-service`, `config-server`,
`account-service`, `payment-service`, `ledger-service`, `fraud-service`, `notification-service`,
`copilot-service`, plus a React `frontend`.

See [`docs/adr/`](docs/adr) for architecture decision records.

## Money safety

Every guarantee below is backed by an automated test — not just a comment.

| Guarantee | Enforced by | Proven by |
|---|---|---|
| Amounts are `BigDecimal`, never `double`/`float` | ArchUnit rule | `NoDoubleOrFloatAmountFieldTest` (architecture-tests) |
| `POST /payments` idempotency (`Idempotency-Key`) | Unique DB constraint + Redis fast path | _Phase 2_ |
| Transactional outbox (never "save then send") | Business row + outbox row, one DB transaction | _Phase 2_ |
| Double-entry ledger sums to zero | Debit + credit per transfer | _Phase 1_ |
| No overdraft under concurrency | Optimistic locking (`@Version`) + retry | _Phase 1_ (50-parallel-transfer test) |
| Kafka consumers are idempotent | `processed_event` table, unique `event_id` | _Phase 2_ |
| Every state change is audited | Append-only audit log | _Phase 7_ |
| Daily reconciliation | Ledger sum vs. cached balance | _Phase 7_ |

## Testing pyramid

<!-- TODO: fill in real counts/links as each phase lands -->

```
        /  E2E (Playwright)  \         <- Phase 4, 6
       /----------------------\
      /  Contract (payment <-> \       <- Phase 2
     /   ledger events)         \
    /------------------------------\
   /  Integration (Testcontainers)  \  <- Phase 1+, one per money-safety rule
  /------------------------------------\
 /  Unit (JUnit 5, Mockito, AssertJ)    \  <- every phase
/------------------------------------------\
```

Coverage gate: JaCoCo fails the build below 70% line coverage (target 80%+ on domain code).

## How to run locally

```bash
git clone https://github.com/jeranjoel-ai/LedgerFlow.git
cd LedgerFlow
make up      # starts Postgres 16, Kafka (KRaft), Redis 7 with healthchecks
make test    # ./mvnw clean verify: Spotless, unit + integration tests, JaCoCo gate
```

Requires: Java 21 (JDK, e.g. Temurin), Docker + Docker Compose. See `.env.example` for
configuration (copy to `.env` and fill in real values — never commit `.env`).

More `make` targets: `make down`, `make lint`, `make seed` (Phase 1+), `make k8s-up` (Phase 8).

## Tech stack

Java 21 · Spring Boot 3.5.x · Spring Cloud Gateway/Config · Kafka (KRaft) · PostgreSQL 16 + Flyway
· Redis 7 · Keycloak · Python/scikit-learn + ONNX Runtime · Spring AI · React 18 + TypeScript ·
Docker Compose · Kubernetes (k3d/k3s) + Helm · Prometheus/Grafana/OpenTelemetry.

Full list with exact versions: [`.github/copilot-instructions.md`](.github/copilot-instructions.md).

## Performance

_Coming in Phase 9_: k6 load test results (throughput, p95 latency) will be published in
[`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).

## Project status

Building phase by phase (0 through 9). See [`docs/SPEC.md`](docs/SPEC.md) for the full plan and
the GitHub Projects board for sprint-level tracking.

- [x] Phase 0: repo setup, build skeleton, local infra, CI skeleton
- [ ] Phase 1: account-service + ledger-service, double-entry posting
- [ ] Phase 2: payment-service, idempotency, outbox, saga
- [ ] Phase 3: Keycloak, gateway, config server, security
- [ ] Phase 4: React frontend, live updates
- [ ] Phase 5: fraud model, fraud-service, analyst review queue
- [ ] Phase 6: AI copilot
- [ ] Phase 7: observability, reconciliation, audit log
- [ ] Phase 8: Kubernetes, CI/CD, deployment
- [ ] Phase 9: load testing, performance tuning, docs polish

## What I'd do next

_To be filled in at the end of Phase 9._

## License

<!-- TODO: pick a license (MIT recommended for a portfolio project) -->
