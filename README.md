# 🚀 Sponti API

Backend service for **Sponti** — a private, invite-only social matching platform that helps friends connect spontaneously based on shared availability.

---

## 🧠 Concept

Sponti is designed to solve a simple problem:

> *“Who among my friends is free right now to hang out?”*

Instead of broadcasting availability publicly, Sponti:

* works only with **your private contact network**
* respects **quiet hours and preferences**
* uses **mutual opt-in matching**
* enables **real-time availability (Free Now)**
* treats matching as an **invitation flow**, not automatic consent to chat or call

---

## 🏗 Architecture

Sponti API is built as a **modular monolith** using **Spring Modulith**, designed for future evolution into microservices.

### Tech Stack

* **Java 21**
* **Spring Boot 4**
* **Spring Modulith**
* **Spring Security (JWT – WIP)**
* **PostgreSQL**
* **Redis** (presence / "Free Now")
* **Flyway** (database migrations)
* **Docker / Docker Compose**
* **Testcontainers** (integration testing)
* **OpenAPI / Swagger**

---

## 📦 Project Structure

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

Each module follows:

```text
module/
├── api        → exposed to other modules
└── internal   → private implementation
```

Spring Modulith enforces strict boundaries between modules.

Current module responsibilities:

* `auth`: JWT authentication and refresh tokens
* `user`: user profile and matching preferences
* `contact`: invitations, accepted contacts, contact metadata
* `availability`: recurring rules, overrides, effective availability
* `matching`: suggestions, proposals, candidate accept/decline, scheduler, matching events
* `notification`: event listeners, notification commands, cooldown/history, placeholder delivery
* `shared`: common enums, errors, and utilities

---

## ⚙️ Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/mgrecuccio/sponti-api.git
cd sponti-api
```

### 2. Start infrastructure

```bash
docker compose up -d
```

This will start:

* PostgreSQL
* Redis
* Elasticsearch
* Kibana
* Filebeat

### 3. Run the application

```bash
./mvnw spring-boot:run
```

---

## 📘 API Documentation

Swagger UI available at:

```
http://localhost:8080/swagger-ui.html
```

---

## 📊 Logging with ELK

The local Docker setup includes a minimal ELK stack for log exploration:

* `elasticsearch`: stores indexed log documents
* `kibana`: UI for searching and filtering logs
* `filebeat`: reads Docker container logs and ships them to Elasticsearch

### `compose.yml`

`compose.yml` starts the following observability services:

* `elasticsearch` on `http://localhost:9200`
* `kibana` on `http://localhost:5601`
* `filebeat`, configured from the repo-level [`filebeat.yml`](filebeat.yml)

The `filebeat` container mounts:

* `./filebeat.yml` as its runtime configuration
* `/var/lib/docker/containers` to read Docker JSON log files
* `/var/run/docker.sock` to enrich events with Docker metadata

### `filebeat.yml`

The Filebeat configuration is intentionally focused on the `sponti-api` container.

Current processing flow:

* read Docker container logs from `/var/lib/docker/containers/*/*.log`
* enrich each event with Docker metadata via `add_docker_metadata`
* drop events that do not belong to the `sponti-api` Docker Compose service
* try to decode JSON content from the `message` field when applicable
* parse request completion logs of the form:

```text
HTTP request completed: method=GET path=/api/v1/auth/login status=200 durationMs=12
```

* extract:
  * `http.request.method`
  * `url.path`
  * `http.response.status_code`
  * `sponti.duration_ms`
  * `sponti.domain`
* add static metadata:
  * `service.name = sponti-api`
  * `environment.name = dev`

`sponti.domain` is derived from the first path segment after `/api/v1/`.

Examples:

* `/api/v1/auth/login` -> `sponti.domain = auth`
* `/api/v1/users/me` -> `sponti.domain = users`
* `/api/v1/contacts` -> `sponti.domain = contacts`
* `/api/v1/availability/rules` -> `sponti.domain = availability`

### Kibana usage

Open Kibana at:

```text
http://localhost:5601
```

Recommended setup:

* create or open the data view for `sponti-api-logs-dev`
* refresh the field list after Filebeat configuration changes
* use Discover to keep only relevant columns such as `@timestamp`, `message`, `service.name`, and `sponti.domain`

Useful filters:

```text
service.name : "sponti-api"
```

```text
sponti.domain : "auth"
```

```text
sponti.domain : ("auth" or "users" or "contacts" or "availability")
```

Important notes:

* field changes in `filebeat.yml` apply only to newly ingested logs
* `sponti.domain` is currently extracted from HTTP request completion log lines
* if you want domain filtering on every application log line, the application should also emit that value directly in structured logs or MDC

---

## 🔐 Authentication

Authentication is being implemented using **JWT (Bearer tokens)**.

Planned flow:

* `POST /auth/register`
* `POST /auth/login`
* `POST /auth/refresh`
* Protected endpoints require Bearer token

---

## 🧪 Testing

Integration tests use **Testcontainers**:

* PostgreSQL container
* Redis container

No local DB setup required.

Useful commands:

```bash
./mvnw -Dtest=ModulithStructureTest test
```

```bash
./mvnw -Dtest=MatchSuggestionsServiceIntegrationTest test
```

---

## 🧩 Key Domain Concepts

* **ContactRelationship**

    * Directional (per-user)
    * Private metadata (nickname, favorite)

* **ContactInvitation**

    * Separate entity for invite workflow

* **Availability**

    * Rules (recurring)
    * Overrides (exceptions)

* **Presence (Redis)**

    * Real-time "Free Now" state
    * Channel-aware (chat/call)

* **MatchSuggestion**

    * Computed on demand
    * Not stored unless the user creates a proposal
    * Ranked by scoring rules

* **MatchProposal**

    * Stored as `PROPOSED`
    * Created by the initiator
    * Accepted or declined by the candidate
    * Emits matching events for notification handling

* **NotificationHistory**

    * Stores sent notification records
    * Supports cooldown and deduplication
    * Currently uses placeholder/log delivery

---

## 🔁 Matching And Notifications

The matching engine uses a proposal/invitation model:

1. `GET /api/v1/matches/suggestions` computes temporary suggestions.
2. `POST /api/v1/matches` creates a stored proposal from `candidateUserId` and `channelType`.
3. The backend recomputes score and overlap server-side before storing.
4. Matching publishes `MatchProposalCreatedEvent`.
5. The notification module listens and sends/logs a `MATCH_PROPOSAL_CREATED` notification.
6. The candidate retrieves proposals with `GET /api/v1/matches/incoming`.
7. The candidate accepts or declines.

Generic suggestion notifications are also supported through a conservative scheduler:

* scheduler evaluates matching-enabled users every configured interval
* suggestions must be current or start before the next scheduler run
* suggestions must pass a higher notification score threshold
* notification history prevents repeated noisy notifications

Push payloads are treated as signals only. Clients should refresh backend endpoints after receiving a notification.

Detailed docs:

* [`docs/4_matching_engine.txt`](docs/4_matching_engine.txt)
* [`docs/9_notification_module.txt`](docs/5_notification_module.txt)

---

## 📌 Roadmap

### Phase 1 — Foundation ✅

* [x] Project setup
* [x] Modular architecture (Spring Modulith)
* [x] Docker + Testcontainers
* [x] Flyway migrations
* [x] Swagger integration

---

### Phase 2 — Authentication ✅

* [x] JWT access token
* [x] Refresh token persistence
* [x] Authentication filter
* [x] `/users/me` endpoint

---

### Phase 3 — Contacts ✅

* [x] Contact invitations
* [x] Accept / decline flow
* [x] Directional relationships
* [x] Block / remove logic
* [x] Contact pending invitations

---

### Phase 4 — Availability ✅

* [x] Recurring availability rules
* [x] Overrides
* [x] Quiet hours
* [x] Redis "Free Now"

---

### Phase 5 — Matching Engine

* [x] Mutual availability detection
* [x] Scoring logic
* [x] Cooldown handling
* [x] Match suggestions
* [x] Stored match proposals
* [x] Candidate-side accept / decline
* [x] Incoming match invitations
* [x] Conservative suggestion scheduler
* [ ] Domain-event-triggered suggestion checks, for example availability rule changes or accepted contacts

---

### Phase 6 — Notifications

* [x] Event-driven integration
* [x] Matching notification listener
* [x] Notification type support
* [x] Notification history table
* [x] Cooldown and deduplication for generic suggestions
* [x] Placeholder/log delivery
* [ ] Real FCM/APNs push delivery
* [ ] Device token management
* [ ] Delivery status and retry handling

---

### Phase 7 — Production Readiness

* [ ] CI/CD pipeline
* [ ] Observability (logs/metrics)
* [ ] Security hardening
* [ ] Deployment (Azure)

---

## 🤝 Contributing

Contributions are welcome!

### Workflow

1. **Fork the repository**

2. **Checkout the `develop` branch**

```bash
git checkout develop
```

3. **Create a feature or bugfix branch**

```bash
git checkout -b feature/my-feature
# or
git checkout -b bugfix/fix-issue
```

4. **Make your changes**

5. **Commit and push**

```bash
git push origin feature/my-feature
```

6. **Open a Pull Request** targeting the `develop` branch

More information in the dedicated **[CONTRIBUTING.md](CONTRIBUTING.md)** file.

---

### Guidelines

* Keep modules isolated (respect Modulith boundaries)
* Do not access `internal` packages from other modules
* Prefer domain events over tight coupling
* Write tests (Testcontainers for integration)
* Keep commits small and meaningful

---

## 🔒 Security Notes

* Never commit secrets (`application.yml`, `.env`)
* Use environment variables for sensitive configuration
* Rotate credentials if exposed

---

## 📜 License

This project is licensed under the **[MIT License](LICENCE)**.

## 📜 Getting Started

Please read the **[Getting Started](GETTING_STARTED.md)** guide to know more about the technical stack.
