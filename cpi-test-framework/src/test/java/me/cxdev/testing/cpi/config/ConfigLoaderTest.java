package me.cxdev.testing.cpi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ConfigLoaderTest {
    @Test
    void missingRequiredFieldThrowsSpecificValidationMessage() throws IOException {
        Path configDir = Files.createDirectories(Path.of("build/test-config/config-missing"));
        Files.writeString(configDir.resolve("cpi-test-config-local.yaml"), """
                cpi:
                  test:
                    monitoringUser: "user"
                    monitoringPassword: "password"
                    collectorBaseUrl: "http://collector"
                """);

        ConfigLoader loader = new ConfigLoader(configDir, Map.of(), new Properties());

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> loader.load(CpiTestProfile.LOCAL));
        assertEquals("Missing required configuration field: monitoringBaseUrl", exception.getMessage());
    }

    @Test
    void environmentVariablesOverrideYamlValues() throws IOException {
        Path configDir = Files.createDirectories(Path.of("build/test-config/config-env"));
        Files.writeString(configDir.resolve("cpi-test-config-local.yaml"), """
                cpi:
                  test:
                    monitoringBaseUrl: "https://yaml.example.test"
                    monitoringUser: "yaml-user"
                    monitoringPassword: "yaml-password"
                    collectorBaseUrl: "http://yaml-collector"
                    pollingIntervalMs: 100
                    pollingTimeoutMs: 200
                """);

        Map<String, String> env = Map.of(
                "CPI_TEST_MONITORING_URL", "https://env.example.test",
                "CPI_TEST_COLLECTOR_URL", "http://env-collector",
                "CPI_TEST_POLLING_TIMEOUT_MS", "999");
        ConfigLoader loader = new ConfigLoader(configDir, env, new Properties());

        CpiTestConfig config = loader.load(CpiTestProfile.LOCAL);

        assertEquals("https://env.example.test", config.getMonitoringBaseUrl());
        assertEquals("http://env-collector", config.getCollectorBaseUrl());
        assertEquals(999, config.getPollingTimeoutMs());
        assertEquals("yaml-user", config.getMonitoringUser());
    }

    @Test
    void profileSwitchingLoadsProfileSpecificFile() throws IOException {
        Path configDir = Files.createDirectories(Path.of("build/test-config/config-profile"));
        Files.writeString(configDir.resolve("cpi-test-config-local.yaml"), """
                cpi:
                  test:
                    monitoringBaseUrl: "https://local.example.test"
                    monitoringUser: "local-user"
                    monitoringPassword: "local-password"
                    collectorBaseUrl: "http://local-collector"
                """);
        Files.writeString(configDir.resolve("cpi-test-config-staging.yaml"), """
                cpi:
                  test:
                    monitoringBaseUrl: "https://staging.example.test"
                    monitoringUser: "staging-user"
                    monitoringPassword: "staging-password"
                    collectorBaseUrl: "http://staging-collector"
                """);

        Properties properties = new Properties();
        properties.setProperty("cpi.test.profile", "staging");
        ConfigLoader loader = new ConfigLoader(configDir, Map.of(), properties);

        CpiTestConfig config = loader.load();

        assertEquals("https://staging.example.test", config.getMonitoringBaseUrl());
        assertEquals("staging-user", config.getMonitoringUser());
    }
}
