package me.cxdev.testing.cpi.extension;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import me.cxdev.testing.cpi.collector.CollectedDocument;
import me.cxdev.testing.cpi.collector.CollectorClient;
import me.cxdev.testing.cpi.communication.CpiCommunicationClient;
import me.cxdev.testing.cpi.communication.CpiSendResult;
import me.cxdev.testing.cpi.config.CpiTestConfig;
import me.cxdev.testing.cpi.ledger.Ledger;
import me.cxdev.testing.cpi.monitoring.CpiMonitoringClient;
import me.cxdev.testing.cpi.monitoring.MonitoringEntry;
import me.cxdev.testing.cpi.residual.ResidualChecker;

public class CpiTestContext {
    private final String runId;
    private final CpiTestConfig config;
    private final Ledger ledger;
    private final CpiMonitoringClient monitoringClient;
    private final CpiCommunicationClient cpiCommunicationClient;
    private final CollectorClient collectorClient;
    private final ResidualChecker residualChecker;
    private final CopyOnWriteArrayList<CollectedDocument> collectedDocuments = new CopyOnWriteArrayList<>();

    public CpiTestContext(
            String runId,
            CpiTestConfig config,
            Ledger ledger,
            CpiMonitoringClient monitoringClient,
            CpiCommunicationClient cpiCommunicationClient,
            CollectorClient collectorClient,
            ResidualChecker residualChecker) {
        this.runId = runId;
        this.config = config;
        this.ledger = ledger;
        this.monitoringClient = monitoringClient;
        this.cpiCommunicationClient = cpiCommunicationClient;
        this.collectorClient = collectorClient;
        this.residualChecker = residualChecker;
    }

    public String getRunId() {
        return runId;
    }

    public CpiTestConfig getConfig() {
        return config;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public CpiMonitoringClient getMonitoringClient() {
        return monitoringClient;
    }

    public CpiCommunicationClient getCpiCommunicationClient() {
        return cpiCommunicationClient;
    }

    public CollectorClient getCollectorClient() {
        return collectorClient;
    }

    public ResidualChecker getResidualChecker() {
        return residualChecker;
    }

    public void recordDocuments(List<CollectedDocument> documents) {
        collectedDocuments.addAll(documents);
    }

    public List<CollectedDocument> getCollectedDocuments() {
        return List.copyOf(collectedDocuments);
    }

    public CpiSendResult sendFileRequestToCpi(String resourcePathOnCpi, String localFilePath) {
        return cpiCommunicationClient.sendFileRequest(resourcePathOnCpi, localFilePath);
    }

    public CpiSendResult sendRawRequestToCpi(String resourcePathOnCpi, String payload) {
        return cpiCommunicationClient.sendRawRequest(resourcePathOnCpi, payload);
    }

    public String getCorrelationIdForMessageId(String messageId) {
        return cpiCommunicationClient.getCorrelationIdForMessageId(messageId, runId);
    }

    public List<MonitoringEntry> waitForCompletion(String correlationId) {
        return cpiCommunicationClient.waitForCompletion(correlationId, runId);
    }

    public List<MonitoringEntry> waitForCompletionForMessageId(String messageId) {
        return cpiCommunicationClient.waitForCompletionForMessageId(messageId, runId);
    }
}
