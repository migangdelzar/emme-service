# EMME Modulith Vision

## Purpose

EMME Modulith is the first production-oriented version of EMME: a multi-tenant SaaS platform for service businesses, beginning with nail salons. It preserves the existing web and Capacitor frontend experience while replacing the distributed Quarkus, Python, and TypeScript backend services with one Java 25 Spring Boot application organized as independently testable Spring Modulith modules.

## Product Outcomes

- Salon teams manage customers, services, schedules, appointments, finances, settings, and tenant-specific subscriptions from the existing frontend.
- Customers discover services, obtain grounded price estimates, ask policy questions, and create bookings through WhatsApp and web chat.
- Platform administrators provision, operate, suspend, reactivate, and eventually deprovision tenants safely.
- AI capabilities use Spring AI and tenant-isolated PostgreSQL retrieval without becoming a source of truth for prices or availability.
- The system remains deployable as one application while preserving module boundaries that allow selected capabilities to be externalized later.

## Primary Actors

| Actor | Responsibility |
|---|---|
| Customer | Uses WhatsApp or web chat to discover services and manage booking requests. |
| Staff member | Manages daily salon operations according to assigned permissions. |
| Artist | Views and fulfills assigned appointments and customer service context. |
| Manager/Owner | Manages the tenant's catalog, customers, schedule, finances, settings, and staff-facing configuration. |
| Platform administrator | Manages tenant lifecycle, subscriptions, entitlements, integrations, and operational health. |
| System operator | Deploys, observes, backs up, restores, and diagnoses the platform. |
| External provider | Supplies identity, messaging, payment, calendar, object-storage, or AI capabilities through controlled adapters. |

## v1 Scope

- Existing React/Vite/Capacitor frontend and shared TypeScript packages, with their GraphQL adapters replaced by REST/OpenAPI adapters.
- Tenant lifecycle, trusted tenant resolution, Keycloak authentication, database-driven authorization, and subscription entitlements.
- Customer, nail-service catalog, appointment, finance, notification, subscription, and payment capabilities.
- Direct Meta WhatsApp Cloud API and a web-chat adapter sharing one conversation core.
- Spring AI orchestration, multimodal normalization, tool calling, tenant-isolated hybrid RAG, and a derived Apache AGE graph projection.
- PostgreSQL with pgvector as the durable source of truth; Redis for expiring coordination data.
- Spring Modulith's JDBC publication registry with Kafka externalization for durable cross-module and integration event streaming.
- Liquibase, Kubernetes/Kustomize, Terraform, GitHub Actions, observability, JUnit, H2, Testcontainers, Playwright, and Locust.

## Explicitly Deferred

- Independently deployed microservices and an internal API gateway process.
- Quarkus, Micronaut, gRPC, protobuf, Python AI services, and TypeScript AI bridges.
- RabbitMQ and AMQP-based externalization; Kafka is the selected event-streaming transport for v1.
- GraphQL, gRPC, service mesh, Argo CD, External Secrets Operator, schema-per-tenant, dedicated databases, fiscal/CFDI, and veterinary workflows.

## Success Definition

The v1 succeeds when every retained frontend and channel use case is served by one deployable Spring Modulith application, tenant isolation is proven against real PostgreSQL, module boundaries are automatically enforced, operational recovery is tested, and the platform can be deployed reproducibly through the retained development and infrastructure toolchain.
