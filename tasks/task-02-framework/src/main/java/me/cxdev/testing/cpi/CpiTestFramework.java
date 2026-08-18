package me.cxdev.testing.cpi;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CpiTestFramework {
    private final CpiTestConfig config;
    private final MonitoringClient monitoringClient;
    private final CollectorGateway collectorGateway;

    public CpiTestFramework(CpiTestConfig config, MonitoringClient monitoringClient, CollectorGateway collectorGateway) {
        this.config = config;
        this.monitoringClient = monitoringClient;
        this.collectorGateway = collectorGateway;
    }

    public CpiScenario scenario(String testCaseName) {
        return new CpiScenario(this, testCaseName);
    }

    public ScenarioResult verify(String testCaseName, String correlationId, List<String> expectedMessageIds) throws Exception {
        waitForCompletion(correlationId);
        List<String> actualMessageIds = resolveMessageIds(correlationId);
        if (!new LinkedHashSet<>(actualMessageIds).equals(new LinkedHashSet<>(expectedMessageIds))) {
            throw new DiagnosticException(
                "Unexpected message ids for test case '" + testCaseName + "': expected=" + expectedMessageIds + ", actual=" + actualMessageIds,
                config.runId(),
                correlationId,
                actualMessageIds,
                "monitoring:/MessageProcessingLogs"
            );
        }
        Map<String, List<Document>> documents = fetchDocuments(actualMessageIds);
        return new ScenarioResult(testCaseName, config.runId(), correlationId, actualMessageIds, documents);
    }

    public void waitForCompletion(String correlationId) throws Exception {
        Instant deadline = Instant.now().plus(config.timeout());
        while (true) {
            List<MonitoringRecord> records = monitoringClient.fetchByCorrelationId(correlationId);
            if (!records.isEmpty() && records.stream().allMatch(record -> "COMPLETED".equals(record.status()))) {
                return;
            }
            if (records.stream().anyMatch(record -> "FAILED".equals(record.status()))) {
                throw new DiagnosticException("CPI processing failed", config.runId(), correlationId,
                    records.stream().map(MonitoringRecord::messageId).toList(), "monitoring:/MessageProcessingLogs");
            }
            if (Instant.now().isAfter(deadline)) {
                throw new DiagnosticException("Timed out while waiting for monitoring completion", config.runId(), correlationId,
                    records.stream().map(MonitoringRecord::messageId).toList(), "monitoring:/MessageProcessingLogs");
            }
            Thread.sleep(config.pollingInterval().toMillis());
        }
    }

    public List<String> resolveMessageIds(String correlationId) throws Exception {
        return monitoringClient.fetchByCorrelationId(correlationId).stream()
            .map(MonitoringRecord::messageId)
            .distinct()
            .toList();
    }

    public Map<String, List<Document>> fetchDocuments(List<String> messageIds) throws Exception {
        Map<String, List<Document>> documents = new LinkedHashMap<>();
        for (String messageId : messageIds) {
            documents.put(messageId, collectorGateway.fetchDocuments(config.runId(), messageId));
        }
        return documents;
    }

    public ResidualState assertNoResidualDocuments(String correlationId, List<String> expectedMessageIds) throws Exception {
        ResidualState residualState = collectorGateway.fetchResidualState(config.runId());
        Set<String> residualMessageIds = residualState.residualMessages().stream()
            .map(ResidualMessage::messageId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        residualMessageIds.removeAll(expectedMessageIds);
        if (!residualMessageIds.isEmpty()) {
            throw new DiagnosticException("Residual documents found: " + residualMessageIds, config.runId(), correlationId,
                List.copyOf(residualMessageIds), "collector:/runs/{runId}/residual");
        }
        return residualState;
    }

    public void releaseAll(List<String> messageIds) throws Exception {
        for (String messageId : messageIds) {
            collectorGateway.releaseMessageGroup(config.runId(), messageId);
        }
    }
}
