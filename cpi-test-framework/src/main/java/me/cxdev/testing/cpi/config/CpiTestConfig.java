package me.cxdev.testing.cpi.config;

public final class CpiTestConfig {
    private final String monitoringBaseUrl;
    private final String monitoringUser;
    private final String monitoringPassword;
    private final String inboundBaseUrl;
    private final String inboundUser;
    private final String inboundPassword;
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
        this(
                monitoringBaseUrl,
                monitoringUser,
                monitoringPassword,
                monitoringBaseUrl,
                monitoringUser,
                monitoringPassword,
                collectorBaseUrl,
                pollingIntervalMs,
                pollingTimeoutMs);
    }

    public CpiTestConfig(
            String monitoringBaseUrl,
            String monitoringUser,
            String monitoringPassword,
            String inboundBaseUrl,
            String inboundUser,
            String inboundPassword,
            String collectorBaseUrl,
            Integer pollingIntervalMs,
            Integer pollingTimeoutMs) {
        this.monitoringBaseUrl = monitoringBaseUrl;
        this.monitoringUser = monitoringUser;
        this.monitoringPassword = monitoringPassword;
        this.inboundBaseUrl = isBlank(inboundBaseUrl) ? monitoringBaseUrl : inboundBaseUrl;
        this.inboundUser = isBlank(inboundUser) ? monitoringUser : inboundUser;
        this.inboundPassword = isBlank(inboundPassword) ? monitoringPassword : inboundPassword;
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

    public String getInboundBaseUrl() {
        return inboundBaseUrl;
    }

    public String getInboundUser() {
        return inboundUser;
    }

    public String getInboundPassword() {
        return inboundPassword;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
