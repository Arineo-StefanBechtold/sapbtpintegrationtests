# CPI Test Framework

Reusable Java/JUnit 5 framework for testing SAP Cloud Integration scenarios.

## Requirements

- JDK 21 or newer
- Gradle 9.7 (the included wrapper is recommended)

## Build

```bash
./gradlew build
```

The default artifact targets JDK 25. The build also creates a JDK 21 variant:

```text
build/libs/integration-testing-1.0.0-SNAPSHOT.jar
build/libs/integration-testing-1.0.0-SNAPSHOT-jdk21.jar
```

The default build requires a locally installed JDK 25. JDK 21 is selected via
Gradle Toolchains for the compatibility variant. The framework is published as
`me.cxdev.sapbtp:integration-testing` and can be consumed by test suites such
as the companion `suite-template` project.
