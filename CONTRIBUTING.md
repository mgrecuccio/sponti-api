# 🤝 Contributing to Sponti API

Thank you for your interest in contributing to **Sponti API** 🚀
We welcome contributions that improve the project, fix bugs, or add new features.

---

## 🧭 Contribution Workflow

All contributions must follow this workflow:

### 1. Fork the repository

Create your own fork of the project on GitHub.

---

### 2. Checkout the `develop` branch

```bash
git checkout develop
```

---

### 3. Create a new branch

Use one of the following naming conventions:

```bash
git checkout -b feature/your-feature-name
git checkout -b bugfix/your-bug-description
```

---

### 4. Implement your changes

* Keep changes focused and minimal
* Follow the existing architecture (Spring Modulith)
* Respect module boundaries (`api` vs `internal`)
* Prefer clean, readable code over clever solutions

---

### 5. Test your changes

* Run the application locally
* Ensure no regressions
* Add or update tests where appropriate (Testcontainers for integration tests)

---

### 6. Commit your work

```bash
git commit -m "feat: add new feature"
```

Use meaningful commit messages:

* `feat:` new feature
* `fix:` bug fix
* `refactor:` code improvement
* `test:` tests
* `docs:` documentation

---

### 7. Push your branch

```bash
git push origin feature/your-feature-name
```

---

### 8. Open a Pull Request

* Target the `develop` branch
* Provide a clear description of your changes
* Reference related issues if applicable

---

## 📌 Guidelines

* ❌ Do not push directly to `main` or `develop`

* ❌ Do not break module boundaries

* ❌ Do not introduce unnecessary dependencies

* ✅ Keep modules independent

* ✅ Prefer domain events over tight coupling

* ✅ Follow existing package structure

* ✅ Keep commits small and focused

---

## 🧪 Testing

The project uses:

* **JUnit 5**
* **Testcontainers** (PostgreSQL, Redis)

Make sure tests pass before submitting a PR.


To keep tests fast and meaningful, pick the smallest scope that validates your change:

* `@ModuleIntegrationTest`
    * Use for integration tests that stay inside a single module.
    * Prefer this by default for module-local behavior.

* `@FullIntegrationTest` (full application context)
    * Use only for end-to-end wiring, application startup checks, and cross-cutting concerns.
    * Keep these tests few, because they are the slowest.
    * Use when a module test needs collaborating modules (shared dependencies).
    * Example: testing `contact` flows that use `user` APIs.

Rule of thumb: start with module-level tests, then expand scope only when required by the behavior under test.

---

## 📒 Logging and Correlation IDs

This project uses a centralized logging pattern configured in `application.yml` so all logs are consistent across modules and environments.

### How the logging pattern works

In `application.yml`, the `logging.level` section defines verbosity by package:

- `root`: global default level (usually `INFO`)
- `com.mgrtech.sponti_api`: application-specific level
- framework packages (`org.springframework`, `org.hibernate`, etc.): typically less verbose (`WARN`)

The `logging.pattern.console` controls the format of each log line.  
Our pattern includes MDC values such as:

- `cid` (`correlationId`): request correlation id
- `tid` / `sid`: trace and span ids when tracing is enabled

If a value is missing, the pattern uses a fallback (e.g. `na`) so logs remain readable.

### Correlation-id logic lives in `JwtAuthenticationFilter`

Correlation id is added in `JwtAuthenticationFilter` because this filter runs once per incoming HTTP request, very early in the chain.  
That makes it the best place to:

1. Read `X-Correlation-Id` from the request (or generate one),
2. Put it in MDC (`correlationId`) so every downstream log line automatically includes it,
3. Return it in response headers to help clients and backend logs align,
4. Clear MDC in `finally` to avoid thread-local leakage between requests.

### Service-level logging guidance

Business classes (like `ContactApplicationService`) should **not** regenerate correlation ids.  
They should just log meaningful domain events (`INFO`), rejected user actions (`WARN`), and diagnostics (`DEBUG`), relying on MDC values already set by the filter.

---

## 🧠 Architecture Notes

Sponti API is a **Spring Modulith application**.

Each module:

* exposes only its `api` package
* keeps implementation in `internal`

Cross-module access must go through `api`.

---

## 💬 Questions or Suggestions?

Feel free to:

* open an issue
* propose improvements
* ask for clarification

---

Thanks for contributing! 🙌
