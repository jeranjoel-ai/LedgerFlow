# GitHub Projects board: one issue per phase

Paste each section below as a separate GitHub Issue (title + body). Suggested labels in
brackets after each title. Add all 10 to a GitHub Projects board (one column per sprint status:
`Backlog` / `In Progress` / `Done`) for 1-week sprints, one phase per sprint-ish (some phases may
span more than one week).

---

## Issue 1: Phase 0 — Repo setup, build skeleton, local infra, CI skeleton
`[phase-0] [infra]`

**Goal:** Parent POM, empty service modules, local dev infra, CI skeleton, README/ADR skeleton.

**Acceptance criteria**
- [x] Parent POM: Java 21, Spring Boot 3.5.x, Spring Cloud BOM, Maven wrapper
- [x] Empty modules for every service + `common` (Money, DomainEvent), each with a context-loads test
- [x] `.gitignore`, `.editorconfig`, `.env.example`; Spotless wired into `verify`
- [x] `docker-compose.yml`: Postgres 16, Kafka (KRaft), Redis 7, with healthchecks; Makefile (`up`/`down`/`test`/`lint`)
- [x] ArchUnit rules: no `double`/`float` `amount` field; controllers must not depend on repositories
- [x] `ci.yml` (Spotless check, `verify`, JaCoCo upload), `dependabot.yml`, `codeql.yml`
- [x] README skeleton + `docs/adr/0001-no-eureka-on-kubernetes.md`
- [x] `./mvnw clean verify` green locally and in CI; no secrets committed

---

## Issue 2: Phase 1 — account-service + ledger-service (double-entry ledger)
`[phase-1] [account-service] [ledger-service] [money-safety]`

**Goal:** Customers/accounts with balances; a double-entry ledger that's the source of truth.

**Acceptance criteria**
- [ ] `account-service`: customers, accounts (CHECKING/SAVINGS), account status, mock KYC status; Flyway migrations; REST API
- [ ] `ledger-service`: every transfer creates exactly two entries (debit + credit) summing to zero; cached balance with `@Version` optimistic locking
- [ ] Unit tests (JUnit 5, Mockito, AssertJ) for domain services
- [ ] Testcontainers Postgres integration tests for both services
- [ ] **Concurrency test**: 50 parallel transfers from the same account (ExecutorService/virtual threads) against Testcontainers Postgres — account is never overdrawn
- [ ] JaCoCo ≥ 70% (target 80%+ on domain code); ArchUnit still green
- [ ] README updated with this phase's architecture; 3 interview questions + model answers

---

## Issue 3: Phase 2 — payment-service: idempotency, outbox, saga, DLTs
`[phase-2] [payment-service] [kafka] [money-safety]`

**Goal:** Accept transfers safely: idempotent, transactional outbox, choreographed saga to ledger.

**Acceptance criteria**
- [ ] `POST /payments` requires `Idempotency-Key`; same key+body replays result, same key+different body returns 422; unique DB constraint (Redis fast path)
- [ ] Transactional outbox: business row + outbox row in one DB transaction; relay publishes to Kafka and marks sent
- [ ] Kafka: idempotent producer, `acks=all`; consumers commit after processing, retry with backoff, DLT + admin replay endpoint
- [ ] Idempotent consumers: `processed_event` table, unique `event_id`
- [ ] Saga: PENDING -> fraud decision (stubbed) -> ledger posts -> COMPLETED/REJECTED, with compensation on ledger failure; Mermaid state machine diagram in docs
- [ ] Contract tests between payment-service and ledger-service events
- [ ] One integration test per money-safety rule above
- [ ] 3 interview questions + model answers

---

## Issue 4: Phase 3 — Keycloak, gateway, config server, security
`[phase-3] [security] [gateway-service] [config-server]`

**Goal:** Real auth: Keycloak roles, gateway JWT validation + rate limiting, centralized config.

**Acceptance criteria**
- [ ] Keycloak realm JSON exported and committed; roles CUSTOMER/ANALYST/ADMIN; Auth Code + PKCE for SPA, client credentials for service-to-service
- [ ] `gateway-service`: routes, JWT validation, CORS, Redis-backed per-user rate limiting, request ID propagation, security headers (CSP, HSTS, X-Content-Type-Options)
- [ ] `config-server`: Git-backed config; threshold configs (e.g. fraud thresholds) externalized
- [ ] Method-level `@PreAuthorize`; test: "user A tries to read user B's account" fails
- [ ] Testcontainers Keycloak integration test
- [ ] 3 interview questions + model answers

---

## Issue 5: Phase 4 — React frontend: customer flows, live updates
`[phase-4] [frontend]`

**Goal:** Customer-facing SPA: accounts, transfers, live balance via SSE, transaction history.

**Acceptance criteria**
- [ ] Install design skills (taste-skill, web design guidelines, image-to-code, awesome-design-md) before starting UI work
- [ ] React 18 + TypeScript + Vite + Tailwind; react-oidc-context login against Keycloak
- [ ] View accounts/balances; live balance updates via SSE; add payees; send transfers; status moves PENDING -> COMPLETED/HELD/REJECTED live
- [ ] Transaction history: filters, pagination, CSV export
- [ ] Vitest + React Testing Library component tests; Playwright smoke test (login -> view balance)
- [ ] Accessible, responsive, bank-grade look; no `dangerouslySetInnerHTML` with user data
- [ ] 3 interview questions + model answers

---

## Issue 6: Phase 5 — Fraud model, fraud-service, analyst review queue
`[phase-5] [fraud-service] [ml]`

**Goal:** Real-time fraud scoring (rules + ONNX model) and the analyst review workflow.

**Acceptance criteria**
- [ ] `/ml`: synthetic transaction dataset generator (normal + injected fraud patterns); train gradient boosting/random forest; `ml/REPORT.md` with precision/recall/PR-AUC; export `model.onnx`, versioned
- [ ] `fraud-service` (Kafka Streams): features (amount, hour, velocity, new payee, amount vs 30-day avg); rules run first (hard blocks), then ONNX model; emits `fraud.decision` with score + top 3 reasons
- [ ] Thresholds configurable via Config Server
- [ ] Analyst review queue: HELD transactions sorted by risk, SLA timer; four-eyes rule above a threshold
- [ ] Fraud dashboard: volume, hold rate, false positive rate, score distribution
- [ ] 3 interview questions + model answers

---

## Issue 7: Phase 6 — AI copilot (Spring AI)
`[phase-6] [copilot-service] [ai]`

**Goal:** Read-only AI explanations for held transactions; human always decides.

**Acceptance criteria**
- [ ] Spring AI 1.x; provider switchable by config only (Gemini/Groq/Ollama)
- [ ] Tool calling limited to read-only repository methods (never raw SQL from the model)
- [ ] Structured output (Java record): explanation + recommended action; copilot never approves/rejects
- [ ] Prompt-injection guard: transaction memos treated as untrusted data; validated structured output; every prompt/response logged for audit
- [ ] Evaluation test: 20 held transactions, checks JSON validity + correct rule-hit mentions; mocked `ChatModel` in CI, real model manual-only workflow
- [ ] 3 interview questions + model answers

---

## Issue 8: Phase 7 — Observability, reconciliation, audit log
`[phase-7] [observability]`

**Goal:** Production-grade visibility: metrics, traces, logs, dashboards, alerts, reconciliation.

**Acceptance criteria**
- [ ] Actuator + Micrometer + Prometheus + Grafana (provisioned dashboard JSON); OpenTelemetry traces to Tempo/Jaeger; one trace spans gateway -> payment -> Kafka -> fraud -> ledger
- [ ] Structured JSON logs with traceId; PII masked (account numbers as `****1234`)
- [ ] Alert rules: Kafka consumer lag > 1000, error rate > 2%
- [ ] Daily reconciliation job: ledger sum vs cached balance per account; mismatches -> alert + `reconciliation_report` row
- [ ] Append-only audit log for every state change
- [ ] 3 interview questions + model answers

---

## Issue 9: Phase 8 — Kubernetes, CI/CD, deployment
`[phase-8] [k8s] [cicd]`

**Goal:** Deploy LedgerFlow to a real cluster with HTTPS and GitOps.

**Acceptance criteria**
- [ ] Helm chart(s): liveness/readiness from Actuator, resource requests/limits, HPA for payment/fraud, ConfigMaps, Secrets, Ingress (Traefik)
- [ ] k3d cluster script for local; k3s on Oracle Cloud Always Free ARM VM for production; multi-arch images (linux/arm64, linux/amd64)
- [ ] Kafka via Strimzi or single-node KRaft StatefulSet; Postgres via CloudNativePG or StatefulSet+PVC
- [ ] `release.yml`: multi-arch build, Trivy scan (fail on CRITICAL), push to GHCR, `helm upgrade --install` (prefer Argo CD)
- [ ] Public HTTPS via Cloudflare Tunnel or Traefik+Let's Encrypt on DuckDNS; frontend on Vercel/Cloudflare Pages
- [ ] Seed script: 3 customers, 1 analyst, 1 admin, 10,000 historical transactions
- [ ] 3 interview questions + model answers

---

## Issue 10: Phase 9 — Load testing, performance tuning, docs, demo
`[phase-9] [performance] [docs]`

**Goal:** Prove it performs, polish the story for interviewers.

**Acceptance criteria**
- [ ] k6: 200 transfers/sec for 2 minutes locally; p95 latency + throughput recorded in `docs/PERFORMANCE.md`
- [ ] Tuning pass: connection pool sizing, indexes, batch inserts, Kafka partitions
- [ ] README final polish: architecture diagram, sequence diagram, saga state machine, money-safety table complete, coverage/CI badges, "What I'd do next"
- [ ] 90-second demo video/GIF; live demo link + read-only demo credentials
- [ ] Runbook (`docs/RUNBOOK.md`): replay a DLT, rotate a secret, `helm rollback`, restore Postgres from backup
- [ ] Uptime monitoring badge (UptimeRobot/Better Stack)
- [ ] 3 interview questions + model answers
