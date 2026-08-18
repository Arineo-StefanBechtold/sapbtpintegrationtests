package me.cxdev.testing.cpi.config;

public final class CpiTestConfig {
    private final String monitoringBaseUrl;
    private final String monitoringUser;
    private final String monitoringPassword;
    private final String collectorBaseUrl;
    private final int pollingIntervalMs;
    private final int pollingTimeoutMs;

    public CpiTestConfig(
            String monitoringBaseUrl,
            String monitoringUser,
            String monitoringPassword,
            String collectorBaseUrl,
            Integer pollingIntervalMs,
            Integer pollingTimeoutMs) {
        this.monitoringBaseUrl = monitoringBaseUrl;
        this.monitoringUser = monitoringUser;
        this.monitoringPassword = monitoringPassword;
        this.collectorBaseUrl = collectorBaseUrl;
        this.pollingIntervalMs = pollingIntervalMs == null ? 5000 : pollingIntervalMs;
        this.pollingTimeoutMs = pollingTimeoutMs == null ? 60000 : pollingTimeoutMs;
    }

    public String getMonitoringBaseUrl() {
        return monitoringBaseUrl;
    }

    public String getMonitoringUser() {
        return monitoringUser;
    }

    public String getMonitoringPassword() {
        return monitoringPassword;
    }

    public String getCollectorBaseUrl() {
        return collectorBaseUrl;
    }

    public int getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public int getPollingTimeoutMs() {
        return pollingTimeoutMs;
    }
}
