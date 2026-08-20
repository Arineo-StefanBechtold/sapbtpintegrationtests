# CPI Test Framework

Reusable Java/JUnit 5 framework for testing SAP Cloud Integration scenarios.

## Requirements

- JDK 21 or newer
- Gradle 9.7 (the included wrapper is recommended)

## Build

```bash
./gradlew test
```

The framework is published as `me.cxdev.sapbtp:integration-testing` and can be
consumed by test suites such as the companion `suite-template` project.
