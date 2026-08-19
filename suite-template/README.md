# Suite Template — CPI Integration Tests

This is a **project-specific test suite template** built on the CPI Test Framework.
Copy this folder into your project, rename it, and adapt it to your integration scenario.

---

## Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Gradle | 8.x (wrapper included) |
| Collector | running on `http://localhost:3000` (or configured via env) |

### Local test run

```bash
# Copy and adapt the example configuration
cp src/test/resources/application-local.yaml src/test/resources/application-local.yaml
# edit the file: fill in your CPI tenant URL and credentials (do NOT commit secrets)

# Run tests
./gradlew test
```

### Update golden masters

```bash
./gradlew test -Dgoldenmaster.update=true
```

> **Warning:** Running in update mode writes the current actual values to `golden-master/`.
> The test will emit a visible warning (or be marked as SKIPPED) so updates are never silent.
> Review the diff carefully before committing updated golden masters.

---

## Project Structure

```
suite-template/
├── build.gradle.kts                          # Gradle build — framework dependency here
├── settings.gradle.kts                       # Project name — rename for your project
└── src/test/
    ├── java/com/example/cpi/suite/
    │   ├── OrderCreatedTest.java             # Example: simple success case
    │   └── OrderCancelledTest.java          # Example: multi-step scenario
    └── resources/
        ├── application-local.yaml           # Local config (placeholders, no secrets)
        ├── junit-platform.properties        # Profile + parallelization settings
        └── testcases/
            ├── order-created/
            │   ├── input.json               # Test input message
            │   └── golden-master/
            │       ├── expected.xml         # Expected output document
            │       └── expected-header.json # Expected processing header
            └── order-cancelled/
                ├── input.json
                └── golden-master/
                    ├── expected.xml
                    └── expected-header.json
```

---

## Adaptation Checklist for a New Project

When copying this template for a new project, work through the following points:

1. **Rename the project** in `settings.gradle.kts` (`rootProject.name`).
2. **Update the framework version** in `build.gradle.kts` if a newer release is available.
3. **Replace example iflow IDs** — search for `TODO: replace with actual iflow name` in the test classes.
4. **Rename testcase folders** to reflect your business scenario (e.g., `invoice-posted/`).
5. **Replace golden master files** with the actual expected output for your integration.
6. **Fill in `application-local.yaml`** with your CPI tenant URL and OAuth credentials.
   Store secrets in environment variables or a secret manager — never commit them.
7. **Enable parallelization** in `junit-platform.properties` if your suite grows large enough to benefit.
8. **Delete or replace example test classes** once you have real test cases.
9. **Update this README** to reflect your project.

---

## Golden Master Workflow

- Golden master files live under `testcases/<name>/golden-master/`.
- They are committed deliberately and act as the expected baseline.
- To update them, run with `-Dgoldenmaster.update=true` and **review the diff** before committing.
- An update run will **not** silently pass — a warning or skip marker is emitted so the update is visible.

---

## Missing Environment Data

If CPI tenant credentials or the Collector URL are not configured, tests will be **skipped** with a clear
diagnostic message rather than failing with a cryptic error. Check the console output for
`[SKIP] Required environment configuration not available` to identify what is missing.

---

## Framework Dependency

The framework is referenced as an external versioned Maven artifact — no framework code is copied here.

```kotlin
testImplementation("me.cxdev.testing:cpi-test-framework:1.0.0-SNAPSHOT")
```

To update the framework, change the version number in `build.gradle.kts`.

---

## Notes

- Testcase folder names and Java class names are **business-facing** (e.g., `order-created`, not `test-001`).
- This template contains **no tenant-specific secrets or hard-coded URLs**.
- The Collector is not part of this project — it is a separate service.
