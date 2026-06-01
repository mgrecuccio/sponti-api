# Getting Started

This guide gives newcomers a practical overview of the Sponti API codebase, technologies, and development practices.

For product/business details, see the documents in `docs/`.

---

## 1. Application Overview

Sponti API is the backend for a private social availability and matching product.

The application supports:

- user registration and JWT authentication
- private contact invitations and accepted contact relationships
- availability rules and overrides
- effective availability calculation
- matching suggestions between accepted contacts
- stored match proposals/invitations
- candidate-owned accept/decline flow
- notification events and placeholder notification delivery
- notification cooldown/history for generic matching suggestions

The backend is intentionally designed as a modular monolith. It should be easy to reason about as one deployable application while keeping module boundaries clear enough for future extraction if needed.

---

## 2. Technology Stack

Main technologies:

- Java 21
- Spring Boot 4
- Spring Modulith
- Spring Web MVC
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Maven
- Docker Compose
- Testcontainers
- OpenAPI / Swagger UI
- Structured JSON logging
- Local ELK stack for log exploration

Testing technologies:

- JUnit 5
- AssertJ
- Mockito
- Spring Boot Test
- Testcontainers for integration tests

---

## 3. Module Structure

Main package:

```text
com.mgrtech.sponti_api
├── auth
├── user
├── contact
├── availability
├── matching
├── notification
└── shared
```

Each business module follows this structure:

```text
module/
├── api        -> public contracts exposed to other modules
└── internal   -> private implementation
```

Rule:
Other modules may depend on `api`, but must not use another module's `internal` package.

Spring Modulith verifies these boundaries through `ModulithStructureTest`.

---

## 4. Module Responsibilities

### auth

Owns authentication concerns:

- registration/login endpoints
- JWT creation and validation
- refresh tokens
- authentication filter

### user

Owns user profile and preferences:

- user persistence
- profile lookup
- matching preferences lookup
- matching-enabled user id query used by the matching scheduler

### contact

Owns contact relationships:

- contact invitations
- accepted contact relationships
- directional contact metadata such as nickname and favorite
- block/remove flows

Matching only considers accepted contacts.

### availability

Owns availability configuration and calculation:

- recurring availability rules
- availability overrides
- effective availability calculation
- channel-aware and channel-agnostic windows

Overrides are channel-agnostic because users do not choose a channel when creating an override.

### matching

Owns matching business rules:

- computes suggestions on demand
- validates hard rules
- calculates score
- creates stored match proposals
- exposes incoming invitations
- candidate-owned accept/decline
- publishes matching events
- runs the conservative matching opportunity scheduler

Matching does not send notifications directly.

### notification

Owns notification delivery concerns:

- listens to matching events
- maps events to notification types
- formats notification commands
- logs placeholder delivery for now
- stores notification history
- applies cooldown and deduplication for generic suggestion notifications

Real FCM/APNs delivery can be added here later.

### shared

Owns shared enums, errors, utilities, and cross-cutting contracts.

---

## 5. Local Setup

### Start Infrastructure

```bash
docker compose up -d
```

This starts local infrastructure such as:

- PostgreSQL
- Redis
- Elasticsearch
- Kibana
- Filebeat

### Run The Application

```bash
./mvnw spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Application logs are structured JSON logs.

Kibana is available locally at:

```text
http://localhost:5601
```

---

## 6. Database And Migrations

Flyway manages schema migrations.

Migration files live in:

```text
src/main/resources/db/migration
```

Practices:

- never edit an already-applied migration in a shared environment
- create a new `V{n}__description.sql` migration for schema changes
- keep entity mappings and migrations aligned
- integration tests validate migrations against PostgreSQL via Testcontainers

Recent matching/notification schema additions include:

- match proposal expiration support
- `notification_history`
- notification metadata for deduplication

---

## 7. Testing

Run targeted unit tests:

```bash
./mvnw -Dtest=ClassNameTest test
```

Run multiple targeted tests:

```bash
./mvnw -Dtest=MatchSuggestionsServiceTest,NotificationApplicationServiceTest test
```

Run Modulith boundary verification:

```bash
./mvnw -Dtest=ModulithStructureTest test
```

Run integration tests using Testcontainers:

```bash
./mvnw -Dtest=MatchSuggestionsServiceIntegrationTest test
```

Integration tests require Docker access because they start PostgreSQL containers.

Recommended practice:

- use unit tests for business rules and edge cases
- use integration tests for persistence, migrations, and Spring wiring
- run `ModulithStructureTest` after changing module dependencies

---

## 8. Matching And Notification Flow

The current matching flow is proposal/invitation based.

High-level flow:

1. User calls `GET /api/v1/matches/suggestions`.
2. Backend computes temporary suggestions on demand.
3. User selects a candidate and channel.
4. User calls `POST /api/v1/matches`.
5. Backend recomputes score and overlap server-side.
6. Backend stores a `PROPOSED` match proposal.
7. Matching publishes `MatchProposalCreatedEvent`.
8. Notification module sends/logs a `MATCH_PROPOSAL_CREATED` notification.
9. Candidate calls `GET /api/v1/matches/incoming`.
10. Candidate accepts or declines.

Important rule:
The initiator creates the proposal. The candidate accepts or declines it.

The scheduler also checks for strong current suggestions:

- runs every configured interval
- uses the existing suggestion algorithm
- only notifies for suggestions starting now or before the next scheduler run
- applies a higher notification score threshold
- notification history suppresses repeated generic notifications

Push payloads are not source of truth. Mobile clients should refresh backend endpoints after receiving a notification.

---

## 9. Configuration Areas

Main configuration file:

```text
src/main/resources/application.yml
```

Important areas:

- `spring.datasource`: PostgreSQL connection
- `spring.data.redis`: Redis connection
- `app.security.jwt`: JWT settings
- `sponti.matching`: matching windows, cooldowns, scoring, scheduler
- `sponti.notification`: notification cooldowns

Current matching scheduler settings:

```yaml
sponti:
  matching:
    opportunity-scheduler:
      enabled: true
      fixed-delay: 5m
```

For multi-instance production deployment, the scheduler needs a distributed lock or a single worker deployment to avoid duplicate scheduled work.

---

## 10. Events And Module Boundaries

The project prefers event-based communication when one module needs to react to another module's business outcome.

Example:

- matching creates a proposal
- matching publishes `MatchProposalCreatedEvent`
- notification listens and sends/logs a notification

This avoids putting notification delivery logic inside the matching module.

Guidelines:

- expose stable contracts in `api`
- keep implementation in `internal`
- prefer domain/application events over direct cross-module calls when reacting to completed business actions
- if a module dependency is necessary, declare it in the module's `package-info.java`
- run `ModulithStructureTest`

---

## 11. Error Handling

The project uses centralized exception handling for shared API behavior and module-level exception handlers for business errors.

Practices:

- use business-specific exceptions for expected domain failures
- return meaningful HTTP status and message to clients
- avoid leaking stack traces or internal implementation details
- treat malformed JSON as a client error

---

## 12. Documentation Map

Useful documents:

- `docs/1_authentication_flow.txt`
- `docs/2_contact_flow.txt`
- `docs/3_availability_rules.txt`
- `docs/4_matching_engine.txt`
- `docs/5_notification_module.txt`

---

## 13. Reference Documentation

Official references:

- [Apache Maven](https://maven.apache.org/guides/index.html)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/4.0.4/maven-plugin)
- [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/4.0.4/reference/testing/testcontainers.html#testing.testcontainers)
- [Testcontainers Postgres Module](https://java.testcontainers.org/modules/databases/postgres/)
- [Spring Web](https://docs.spring.io/spring-boot/4.0.4/reference/web/servlet.html)
- [Spring Security](https://docs.spring.io/spring-boot/4.0.4/reference/web/spring-security.html)
- [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.4/reference/data/sql.html#data.sql.jpa-and-spring-data)
- [Flyway Migration](https://docs.spring.io/spring-boot/4.0.4/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
- [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)
- [Testcontainers](https://java.testcontainers.org/)

---

## 14. Maven Parent Overrides

Due to Maven inheritance, some metadata elements are inherited from the parent POM.

The project POM contains empty overrides for elements such as `<license>` and `<developers>` to avoid inheriting unwanted parent metadata.

If the parent POM changes and inherited metadata becomes desired, remove those overrides intentionally.
