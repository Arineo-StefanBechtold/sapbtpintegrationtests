plugins {
    java
}

group = "me.cxdev.testing"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    testImplementation(project(":task-02-framework"))
    testImplementation(project(":task-03-suite-template"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    testImplementation("org.yaml:snakeyaml:2.3")
    testImplementation("org.wiremock:wiremock:3.9.2")
}

tasks.test {
    useJUnitPlatform()
}
