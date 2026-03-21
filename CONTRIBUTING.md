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
