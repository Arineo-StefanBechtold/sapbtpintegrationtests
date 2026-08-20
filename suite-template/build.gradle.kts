plugins {
    java
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Framework as external versioned dependency — do NOT copy framework code into this project
    testImplementation("me.cxdev.sapbtp:integration-testing:1.0.0-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("goldenmaster.update", System.getProperty("goldenmaster.update", "false"))
}
