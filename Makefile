SHELL := /bin/zsh
JAVA_HOME_21 := /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
MVNW := JAVA_HOME=$(JAVA_HOME_21) ./mvnw

.PHONY: up down test lint seed k8s-up logs ps

## Start local infra (Postgres, Kafka, Redis) and wait for health
up:
	docker compose up -d
	@echo "Waiting for services to become healthy..."
	@until [ "$$(docker compose ps --format '{{.Health}}' | grep -vc healthy)" = "0" ]; do \
		sleep 2; \
	done
	@echo "All services healthy."

## Stop and remove local infra containers (keeps volumes)
down:
	docker compose down

## Run the full Maven build: Spotless check, unit + integration tests, JaCoCo gate
test:
	$(MVNW) clean verify

## Apply code formatting (Spotless) across all modules
lint:
	$(MVNW) spotless:apply

## Seed demo data (added in a later phase)
seed:
	@echo "make seed: not implemented yet (added when account/payment services exist)"

## Bring up a local k3d cluster (added in Phase 8)
k8s-up:
	@echo "make k8s-up: not implemented yet (added in Phase 8)"

## Tail logs from local infra
logs:
	docker compose logs -f

## Show status of local infra containers
ps:
	docker compose ps
