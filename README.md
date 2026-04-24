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

* [ ] Mutual availability detection
* [ ] Scoring logic
* [ ] Cooldown handling
* [ ] Match suggestions

---

### Phase 6 — Notifications

* [ ] Push notifications
* [ ] Real-time triggers
* [ ] Event-driven integration

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
