You are my senior engineer and mentor. Help me build a portfolio project from an empty repo to a deployed, tested, documented system. I will build it phase by phase. For each phase: explain the design briefly, generate the code, tell me exactly which commands to run, write the tests, and stop so I can commit before moving on. Never skip tests. Prefer explaining WHY over long code dumps. If you are unsure about a library version, tell me to check the latest stable version instead of guessing.

## About me
Java backend engineer, 2.5 years at Cognizant (Java/Spring Boot microservices, Kafka, React, JPA/Hibernate, SQL tuning). MS CS from UT Arlington, AWS Solutions Architect Associate. I'm targeting Java backend roles at banks and fintech in Dallas-Fort Worth, so I want this to look and behave like a real bank system. Everything must be free to run (free tiers, open-source tools).

## Project: LedgerFlow
A real-time payments platform. Customers hold accounts, send money to each other, and see their balance update live. Every money movement is recorded in a double-entry ledger. Every transaction is scored for fraud in real time; suspicious ones are held for review. A fraud analyst dashboard shows held transactions with an AI-generated explanation, and the analyst approves or rejects them.

## Tech stack (use these exact choices)
- Java 21 (LTS), Spring Boot 3.5.x, Maven multi-module build with a parent POM and the Maven wrapper
- Spring Web (REST), Spring Data JPA / Hibernate, Bean Validation, Spring Security (OAuth2 Resource Server, JWT)
- Spring Cloud Gateway (API gateway), Spring Cloud Config Server (centralized config, Git-backed). No Eureka: on Kubernetes, service discovery comes from K8s Services; explain this trade-off in docs/adr.
- Apache Kafka (KRaft mode, no ZooKeeper) with Spring for Apache Kafka and Kafka Streams
- PostgreSQL 16 with Flyway migrations (one database per service, separate schemas are acceptable locally)
- Redis 7 (rate limiting, velocity counters, idempotency cache)
- Keycloak (open-source identity provider; OAuth2/OIDC, realm exported to JSON and committed)
- Python 3.12 + scikit-learn + skl2onnx for training the fraud model offline; ONNX Runtime for Java for inference
- Spring AI 1.x for the LLM analyst copilot. Default provider: Google Gemini free tier OR Groq free tier through the OpenAI-compatible API; Ollama for fully local runs. The provider must be switchable by config only.
- React 18 + TypeScript + Vite + Tailwind CSS + Axios + TanStack Query + Recharts; react-oidc-context for login
- Docker + Docker Compose for local dev; Kubernetes (k3d locally, k3s in production) with Helm charts
- Observability: Spring Boot Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry tracing to Grafana Tempo (or Jaeger locally)
- Testing: JUnit 5, Mockito, AssertJ, Testcontainers (Postgres, Kafka, Redis, Keycloak), Spring Cloud Contract or Pact for contract tests, ArchUnit, k6 for load tests, Vitest + React Testing Library + Playwright for the UI
- CI/CD: GitHub Actions, GitHub Container Registry (GHCR), Trivy image scan, CodeQL, Dependabot, JaCoCo coverage gate, Spotless (google-java-format)

## Services (each its own Maven module and Docker image)
1. gateway-service: Spring Cloud Gateway. Routes, JWT validation, CORS, Redis-backed rate limiting per user (RequestRateLimiter), request ID propagation.
2. config-server: Spring Cloud Config Server pointing at a /config folder in the repo (or a separate config repo).
3. account-service: customers and accounts (CHECKING, SAVINGS), account status, KYC status (mock). Owns balances as a read model.
4. payment-service: accepts transfer requests, validates them, writes a PENDING transfer + outbox event in ONE database transaction, and publishes via the outbox relay.
5. ledger-service: the source of truth. Double-entry journal: every transfer creates exactly two entries (debit + credit) that sum to zero. Balances are derived from entries; also keep a cached balance column updated with optimistic locking (@Version).
6. fraud-service: Kafka Streams app. Consumes payment events, computes features (amount, hour, velocity in last 5 min / 1 hour from Redis or a state store, new payee flag, amount vs. 30-day average), runs rules + the ONNX model, emits fraud.decision events (APPROVE / REVIEW / BLOCK) with a score and the top reasons.
7. notification-service: consumes events and pushes live updates to the browser using Server-Sent Events (SSE); stores a notification inbox; sends email through a free SMTP service (Mailpit locally).
8. copilot-service (can live inside fraud-service at first): Spring AI. Given a held transaction, it gathers facts via tool calling (customer history, recent transactions, rule hits, model score) and writes a short explanation and a recommended action as structured JSON (Java record). It must NEVER approve anything by itself; a human clicks the button.
9. frontend: React app with two roles: CUSTOMER and ANALYST (plus ADMIN).

## Money-safety requirements (these are the interview gold; implement and test every one)
- Amounts are BigDecimal / NUMERIC(19,4) with a currency code. Never double/float. Add an ArchUnit rule that fails the build if a field named amount is a double.
- Idempotency: POST /payments requires an Idempotency-Key header. Same key + same body returns the original result; same key + different body returns 422. Store keys in Postgres with a unique constraint (Redis as a fast path).
- Transactional outbox: payment row and outbox row in the same DB transaction; a relay publishes outbox rows to Kafka and marks them sent. Explain why this beats "save then send".
- Kafka: idempotent producer, acks=all, consumer offsets committed after processing, retries with backoff, dead-letter topics (DLT) with a replay endpoint for admins.
- Consumers are idempotent (processed_event table with unique event_id).
- Saga for transfer: payment PENDING -> fraud decision -> ledger posts entries -> payment COMPLETED; or REJECTED with reason. Compensation if ledger posting fails. Use choreography with events and document the state machine in a Mermaid diagram.
- Concurrency: two simultaneous transfers from the same account must never overdraw it. Use optimistic locking with retry, and prove it with a test that fires 50 parallel transfers using an ExecutorService (or virtual threads) against Testcontainers Postgres.
- Daily reconciliation job (Spring @Scheduled or Spring Batch): sum of all ledger entries per account must equal cached balance; mismatches raise an alert and an entry in a reconciliation_report table.
- Audit log: every state change stored with who/when/what (append-only table).

## Fraud model (Python, in /ml folder)
- Generate a synthetic but realistic transaction dataset with a Python script (normal behavior per customer, plus injected fraud patterns: card-testing bursts, unusual hours, large first-time payee transfers, account takeover).
- Train a gradient boosting or random forest classifier, report precision/recall/PR-AUC in ml/REPORT.md, export to model.onnx, and version it (model_version field in every fraud decision).
- Java loads the ONNX model at startup. Rules run first (hard blocks, e.g. amount > daily limit), then the model. Return the top 3 reasons (rule hits + strongest features).
- Make thresholds configurable via Config Server so they can change without redeploying.

## Features list
Customer:
- Sign up / log in (Keycloak), view accounts and balances, live balance updates via SSE
- Add payees, send transfers, see transfer status move PENDING -> COMPLETED / HELD / REJECTED in real time
- Transaction history with filters, pagination, CSV export, monthly statement PDF (OpenPDF)
- Spending insights chart (by category and month)
Analyst:
- Review queue of HELD transactions sorted by risk score, with SLA timer
- Transaction detail: rule hits, model score, feature values, customer's last 20 transactions, AI explanation
- Approve / Reject with mandatory comment; four-eyes rule for amounts above a threshold (second analyst must confirm)
- Fraud dashboard: volume, hold rate, false positive rate (from analyst decisions), model score distribution
- "Ask the copilot" box: natural-language questions like "show me all held transfers over $5,000 this week" answered through tool calling over read-only repository methods (never raw SQL from the model)
Admin:
- Dead-letter topic viewer and replay, reconciliation report, feature flags, rule threshold editor, audit log search

## API design
- REST, versioned under /api/v1, OpenAPI 3 docs via springdoc (Swagger UI exposed through the gateway)
- RFC 7807 Problem Details for every error via a global @RestControllerAdvice
- DTOs as Java records; MapStruct for mapping; never expose entities
- Cursor-based pagination for transaction history
- Provide a Postman collection + environment in /postman and a Newman run in CI

## Security (treat as a bank would)
- Keycloak realm with roles CUSTOMER, ANALYST, ADMIN; Authorization Code + PKCE for the SPA; client credentials for service-to-service calls
- Every service is a resource server; method-level security (@PreAuthorize); customers can only ever see their own accounts (test this with a "user A tries to read user B's account" test)
- Input validation on every DTO; output encoding in React; strict CORS; security headers (CSP, HSTS, X-Content-Type-Options) at the gateway
- Rate limiting per user and per IP; account lockout handled by Keycloak brute-force detection
- Secrets never in Git: .env for local (gitignored, with .env.example committed), Kubernetes Secrets (Sealed Secrets or SOPS) in production
- PII masking in logs (account numbers shown as ****1234) using a Logback converter
- Prompt-injection guard for the copilot: transaction memos are untrusted text; wrap them as data, limit tools to read-only methods, validate structured output, and log every prompt/response for audit
- OWASP Dependency-Check or Trivy fs scan in CI; Dependabot weekly; CodeQL on every PR
- Write SECURITY.md with a threat model table (STRIDE, one row per threat, mitigation, where it's tested)

## Testing strategy (show the pyramid in the README)
- Unit: services and fraud feature calculation with Mockito; target 80%+ line coverage on domain code (JaCoCo gate fails the build below 70%)
- Integration: @SpringBootTest + Testcontainers for Postgres/Kafka/Redis; one test per money-safety rule above
- Contract tests between payment-service and ledger-service events
- Architecture tests with ArchUnit (no controller talks to a repository directly; no double amounts)
- End-to-end: Playwright script: log in as customer, send money, log in as analyst, approve, customer sees COMPLETED
- Load: k6 script for 200 transfers/second for 2 minutes locally; record p95 latency and throughput in docs/PERFORMANCE.md (these become resume numbers I can defend)
- AI: a small evaluation set (20 held transactions with expected key facts); a test checks the copilot's JSON is valid and mentions the right rule hits. Use a mocked ChatModel in CI and the real model only in a manual workflow.

## Local developer experience
- `docker compose up` starts Postgres, Kafka (KRaft), Redis, Keycloak (with imported realm), Mailpit, Prometheus, Grafana (with a provisioned dashboard JSON), Jaeger, and all services
- A seed script creates 3 demo customers, 1 analyst, 1 admin, and 10,000 historical transactions so dashboards look alive
- Makefile targets: make up, make down, make test, make seed, make lint, make k8s-up

## Kubernetes and deployment
- Helm chart per service (or one umbrella chart) with liveness/readiness probes from Actuator, resource requests/limits, HorizontalPodAutoscaler for payment and fraud services, ConfigMaps, Secrets, and an Ingress (Traefik, which ships with k3s)
- Local: k3d cluster script. Production: k3s on an Oracle Cloud Always Free ARM VM (multi-arch images built with docker buildx for linux/arm64 and linux/amd64)
- Kafka on K8s via the Strimzi operator (or a single-node KRaft StatefulSet to save memory); Postgres via the CloudNativePG operator or a simple StatefulSet with a PVC
- Public HTTPS: Cloudflare Tunnel (free) or Caddy/Traefik with Let's Encrypt on a free DuckDNS subdomain
- Frontend deployed separately on Vercel or Cloudflare Pages (free), pointing at the API domain

## CI/CD (GitHub Actions)
- ci.yml on every PR: build, Spotless check, unit + integration tests (Testcontainers works on GitHub runners), JaCoCo report, Newman API tests, frontend lint + tests, CodeQL
- release.yml on merge to main: build multi-arch images, Trivy scan (fail on CRITICAL), push to GHCR tagged with the git SHA, then deploy with `helm upgrade --install` over SSH or via a self-hosted runner / Argo CD on the cluster (prefer Argo CD for a GitOps story)
- Branch protection: PRs required, CI must pass; conventional commits; release notes generated automatically

## Maintenance and operations
- Grafana dashboards: transfers/sec, p95 latency per endpoint, Kafka consumer lag, fraud hold rate, JVM memory/GC; alert rules for consumer lag > 1000 and error rate > 2% (Grafana alerts to email/Discord webhook)
- Structured JSON logs with traceId; one trace that follows a transfer across gateway -> payment -> Kafka -> fraud -> ledger
- Runbook in docs/RUNBOOK.md: how to replay a DLT, rotate a secret, roll back a release (helm rollback), restore Postgres from backup (nightly pg_dump CronJob to object storage)
- Uptime monitoring with UptimeRobot or Better Stack free tier; status badge in the README
- Dependabot + a monthly "upgrade day" checklist; ADRs in docs/adr for every big decision (outbox vs. CDC, no Eureka on K8s, Kafka Streams vs. plain consumers, ONNX vs. calling a Python service)

## Documentation (the README is what interviewers read)
- One-line pitch, live demo link, demo login credentials for a read-only demo customer and analyst, 90-second demo video/GIF
- Architecture diagram (Mermaid or Excalidraw PNG), event flow sequence diagram, saga state machine
- "Money safety" section listing each guarantee and the test that proves it
- Performance numbers from k6, fraud model metrics, test coverage badge, CI badge
- How to run locally in one command; "What I'd do next" section

## Build phases (stop after each one so I can commit)
Phase 0: repo setup: parent POM, modules, Maven wrapper, .gitignore, .editorconfig, Spotless, docker-compose with Postgres + Kafka + Redis, GitHub Actions skeleton, README skeleton, GitHub Projects board with issues for each phase (Agile: I'll work in 1-week sprints).
Phase 1: account-service + ledger-service with double-entry posting, Flyway, REST API, unit + Testcontainers tests, the 50-parallel-transfers concurrency test.
Phase 2: payment-service with idempotency keys, outbox, Kafka events, saga to ledger, DLTs, idempotent consumers.
Phase 3: Keycloak + gateway + config server; roles; rate limiting; security tests.
Phase 4: React app: customer flows, live updates via SSE, transaction history.
Phase 5: Python fraud model, ONNX export, fraud-service with Kafka Streams, rules + model, analyst review queue with four-eyes rule.
Phase 6: AI copilot with Spring AI: explanations as structured output, read-only tool calling, prompt-injection guard, evaluation test.
Phase 7: observability (metrics, traces, logs, dashboards, alerts) + reconciliation job + audit log.
Phase 8: Helm charts, k3d locally, CI/CD to GHCR, deploy to Oracle Cloud k3s with HTTPS, seed demo data.
Phase 9: k6 load test, performance tuning (connection pool sizing, indexes, batch inserts, Kafka partitions), docs, demo video, final README polish.

Definition of done for every phase: code compiles, all tests pass locally and in CI, README updated, no secrets committed, and you (the assistant) give me 3 interview questions about what we just built with short model answers.

it should look like how the enterprise application works feature by feature
push to the git also test each feature and push those too , i have copilot setup which help me with the git too . try to make use of the ai skills also it matches naming convetions and design pattern

try to use this skill in my project :
npx skills add https://github.com/Leonxlnx/taste-skill --skill "design-taste-frontend"

Taste Skill : https://www.tasteskill.dev/ 

21st MCP : https://github.com/21st-dev/magic-mcp

Web Design Guidelines : https://github.com/vercel-labs/agent-skills/blob/main/skills/web-design-guidelines/SKILL.md

Image to Code : https://github.com/Leonxlnx/taste-skill/blob/main/skills/image-to-code-skill/SKILL.md

Awesome Design : https://github.com/VoltAgent/awesome-design-md/

Playwright CLI : https://github.com/microsoft/playwright-cli


try to include these skills also include whateve feature is possible
