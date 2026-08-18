package me.cxdev.testing.cpi.monitoring;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.cxdev.testing.cpi.config.CpiTestConfig;
import me.cxdev.testing.cpi.http.HttpResponse;
import me.cxdev.testing.cpi.http.HttpTransport;
import okhttp3.Credentials;

public class CpiMonitoringClient {
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final HttpTransport transport;
    private final CpiTestConfig config;
    private final MonitoringParser parser;
    private final Sleeper sleeper;

    public CpiMonitoringClient(HttpTransport transport, CpiTestConfig config) {
        this(transport, config, new MonitoringParser(), Thread::sleep);
    }

    public CpiMonitoringClient(HttpTransport transport, CpiTestConfig config, MonitoringParser parser, Sleeper sleeper) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.config = Objects.requireNonNull(config, "config");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    public List<String> fetchMessageIds(String correlationId, String runId) {
        long deadline = System.currentTimeMillis() + config.getPollingTimeoutMs();
        while (true) {
            HttpResponse response = transport.get(buildUrl(correlationId), buildHeaders());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Monitoring API returned status " + response.statusCode() + " for correlationId=" + correlationId + ", runId=" + runId);
            }
            List<String> messageIds = parser.parse(response.body()).messageIds();
            if (!messageIds.isEmpty()) {
                return messageIds;
            }
            if (System.currentTimeMillis() >= deadline) {
                return List.of();
            }
            sleepQuietly(correlationId, runId);
        }
    }

    private String buildUrl(String correlationId) {
        String baseUrl = config.getMonitoringBaseUrl().endsWith("/")
                ? config.getMonitoringBaseUrl().substring(0, config.getMonitoringBaseUrl().length() - 1)
                : config.getMonitoringBaseUrl();
        String filter = URLEncoder.encode("CorrelationId eq '" + correlationId + "'", StandardCharsets.UTF_8);
        return baseUrl + "/api/v1/MessageProcessingLogs?$filter=" + filter + "&$format=json";
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", Credentials.basic(config.getMonitoringUser(), config.getMonitoringPassword()));
        headers.put("Accept", "application/json");
        return headers;
    }

    private void sleepQuietly(String correlationId, String runId) {
        try {
            sleeper.sleep(config.getPollingIntervalMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Monitoring polling interrupted for correlationId=" + correlationId + ", runId=" + runId,
                    e);
        }
    }
}
