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

### Phase 3 — Contacts 🔄

* [ ] Contact invitations
* [ ] Accept / decline flow
* [ ] Directional relationships
* [ ] Block / remove logic

---

### Phase 4 — Availability

* [ ] Recurring availability rules
* [ ] Overrides
* [ ] Quiet hours
* [ ] Redis "Free Now"

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