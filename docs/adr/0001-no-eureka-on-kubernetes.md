# 1. No Eureka service discovery on Kubernetes

## Status
Accepted

## Context
The spec lists Spring Cloud Netflix Eureka as a common choice for service discovery in a
Spring Cloud microservices system, and LedgerFlow does use Spring Cloud Gateway and Spring
Cloud Config Server. However, LedgerFlow's target deployment is Kubernetes (k3d locally, k3s
in production via Helm charts), not a bare VM or Cloud Foundry-style PaaS.

Kubernetes already provides:
- A built-in Service abstraction with a stable virtual IP and DNS name
  (`<service-name>.<namespace>.svc.cluster.local`) that load-balances across healthy Pods.
- `livenessProbe`/`readinessProbe` wired to Spring Boot Actuator health endpoints, which K8s
  uses to decide whether a Pod should receive traffic at all.
- Automatic re-registration on Pod restart/rescheduling - there's no separate registry to
  keep in sync with the actual running Pods.

Running Eureka on top of this would mean maintaining a second, parallel source of truth for
"who is alive and where," which can drift from what Kubernetes itself already knows (e.g. a Pod
marked healthy by Kubernetes but not yet re-registered with Eureka, or vice versa during a
rolling update).

## Decision
Do not run a Eureka server. Services resolve each other via Kubernetes DNS/Service names.
Spring Cloud LoadBalancer (or plain `RestClient`/`WebClient` pointed at the Service's cluster
DNS name) replaces Eureka-based client-side discovery. Spring Cloud Gateway routes are
configured with Kubernetes Service hostnames instead of a `lb://SERVICE-ID` Eureka URI.

## Consequences
- One fewer stateful component to run, patch, and keep highly available locally and in
  production (relevant since the production target is a single Oracle Cloud Always Free ARM
  VM running k3s - resources are constrained).
- Service discovery behavior is identical between a developer's local k3d cluster and the
  production k3s cluster, since both are real Kubernetes.
- Loses a few Eureka-specific features (e.g. its self-preservation mode during network
  partitions, or non-K8s deployments reusing the same registry). Not a concern here because
  LedgerFlow never runs outside Kubernetes once past `docker compose up` for local dev.
- If a non-Kubernetes deployment target is ever added (e.g. plain VMs without an orchestrator),
  this decision should be revisited - Eureka (or Consul) would then earn its keep again.
