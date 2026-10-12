# LedgerFlow: Copilot instructions

LedgerFlow is a real-time payments platform built to look and behave like a real bank system.
The full spec is in `docs/SPEC.md`. Read it before any non-trivial change and follow it over these notes if they ever disagree.

## How to work with me
- Work phase by phase (Phase 0 to 9 in `docs/SPEC.md`). Never start the next phase until the current one is committed.
- Within a phase, do one small piece at a time (one module, one feature, or one rule) together with its tests.
- For each piece: explain the design briefly (WHY over long code), generate the code, write the tests, list the exact commands to run, then stop so I can run them and commit.
- Never skip tests. A feature without a test is not done.
- If you are unsure of a library version, say so and tell me to check the latest stable version. Do not guess version numbers.
- Everything must be free to run: open-source tools and free tiers only.
- Commit messages use Conventional Commits (`feat(ledger): ...`, `test(payment): ...`, `fix:`, `docs:`, `chore:`, `ci:`). One feature plus its tests per commit.
- At the end of every phase, give me 3 interview questions about what we built, with short model answers.

## Stack (exact choices, do not substitute)
- Java 21, Spring Boot 3.5.x, Maven multi-module with a parent POM and the Maven wrapper (`./mvnw`).
- Spring Web, Spring Data JPA, Bean Validation, Spring Security (OAuth2 Resource Server, JWT).
- Spring Cloud Gateway and Spring Cloud Config Server. No Eureka; Kubernetes Services do discovery.
- Kafka in KRaft mode, Spring for Apache Kafka, Kafka Streams.
- PostgreSQL 16 with Flyway (one DB per service; separate schemas OK locally). Redis 7.
- Keycloak (realm exported to JSON and committed).
- Python 3.12 + scikit-learn + skl2onnx offline; ONNX Runtime for Java at inference.
- Spring AI 1.x; LLM provider switchable by config only (Gemini or Groq via OpenAI-compatible API, Ollama locally).
- Frontend: React 18, TypeScript, Vite, Tailwind, Axios, TanStack Query, Recharts, react-oidc-context.
- Docker Compose locally; Kubernetes (k3d local, k3s prod) with Helm.
- Observability: Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry to Tempo or Jaeger.
- Tests: JUnit 5, Mockito, AssertJ, Testcontainers, Spring Cloud Contract or Pact, ArchUnit, k6, Vitest, React Testing Library, Playwright.
- CI/CD: GitHub Actions, GHCR, Trivy, CodeQL, Dependabot, JaCoCo gate, Spotless (google-java-format).

## Money-safety rules (never break these)
- Money is `BigDecimal` with a currency code; DB columns are `NUMERIC(19,4)`. Never `double` or `float` for money. An ArchUnit rule enforces this.
- `POST /payments` requires an `Idempotency-Key` header. Same key + same body returns the original result; same key + different body returns 422. Keys stored in Postgres with a unique constraint; Redis is only a fast path.
- Transactional outbox: the business row and the outbox row are written in ONE DB transaction. A relay publishes outbox rows to Kafka and marks them sent. Never "save then send".
- Kafka producers: idempotent, `acks=all`. Consumers: commit offsets after processing, retry with backoff, dead-letter topics, admin replay endpoint.
- Consumers are idempotent via a `processed_event` table with a unique `event_id`.
- Transfers follow a choreographed saga: PENDING -> fraud decision -> ledger posts -> COMPLETED, or REJECTED with reason, with compensation if ledger posting fails.
- Ledger is double-entry: every transfer creates exactly two entries (debit + credit) that sum to zero. Cached balances use optimistic locking (`@Version`) with retry. No account can be overdrawn under concurrency.
- Every state change goes to an append-only audit log (who, when, what).
- The AI copilot never approves or rejects anything. A human does. Copilot tools are read-only; transaction memos are untrusted data.

## Code conventions and design patterns
- Package by feature inside each service: `com.ledgerflow.<service>.<feature>` with `api` (controllers, DTOs), `domain` (entities, domain services), `infra` (repositories, Kafka, external clients).
- Layering: Controller -> Service -> Repository. Controllers never call repositories directly (ArchUnit enforces this).
- DTOs are Java `record`s with Bean Validation annotations. Map with MapStruct. Never expose JPA entities from controllers.
- Constructor injection only (no field `@Autowired`). Prefer immutable value objects (e.g. a `Money` record).
- Errors: one global `@RestControllerAdvice` returning RFC 7807 `ProblemDetail`. Custom exceptions per domain error.
- REST paths under `/api/v1`, plural nouns, kebab-case (`/api/v1/accounts/{id}/transactions`). OpenAPI via springdoc.
- Cursor-based pagination for transaction history.
- Naming: classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`, DB tables and columns `snake_case`, Kafka topics `domain.event` (e.g. `payment.created`, `fraud.decision`), Flyway files `V1__create_accounts.sql`.
- Events are versioned records with `eventId`, `occurredAt`, `type`, and payload.
- Method-level security with `@PreAuthorize`. Customers can only see their own data.
- Mask PII in logs (account numbers as `****1234`). Never log secrets. Secrets come from `.env` (gitignored) locally; `.env.example` is committed.
- Format with Spotless (google-java-format). Run `./mvnw spotless:apply` before committing.

## Testing rules
- Unit tests with JUnit 5, Mockito, AssertJ. Test names describe behavior: `shouldReturn422WhenIdempotencyKeyReusedWithDifferentBody`.
- Integration tests with `@SpringBootTest` + Testcontainers (Postgres, Kafka, Redis, Keycloak). One integration test per money-safety rule.
- JaCoCo gate fails the build below 70% line coverage; aim for 80%+ on domain code.
- Contract tests between payment-service and ledger-service events.
- In CI the LLM is always a mocked `ChatModel`.
- Frontend: Vitest + React Testing Library for components, Playwright for end-to-end.

## Frontend design
- Use the design skills listed at the bottom of `docs/SPEC.md` (design-taste-frontend, web design guidelines, image-to-code, awesome-design-md, 21st magic MCP) for UI work.
- Accessible, responsive, bank-grade look. Encode all output; never use `dangerouslySetInnerHTML` with user data.

## Definition of done (every phase)
Code compiles, all tests pass locally and in CI, README updated, no secrets committed, 3 interview questions with model answers.
