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
    testImplementation("me.cxdev.testing:cpi-test-framework:1.0.0-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("goldenmaster.update", System.getProperty("goldenmaster.update", "false"))
}
