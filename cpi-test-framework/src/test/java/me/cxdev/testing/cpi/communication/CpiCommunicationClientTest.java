package me.cxdev.testing.cpi.communication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import me.cxdev.testing.cpi.config.CpiTestConfig;
import me.cxdev.testing.cpi.http.OkHttpTransport;
import me.cxdev.testing.cpi.monitoring.CpiMonitoringClient;
import me.cxdev.testing.cpi.monitoring.MonitoringEntry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class CpiCommunicationClientTest {
    @Test
    void sendRawRequestPostsPayloadAndExtractsMessageIdFromHeaders() throws Exception {
        try (MockWebServer inboundServer = new MockWebServer();
                MockWebServer monitoringServer = new MockWebServer()) {
            inboundServer.enqueue(new MockResponse()
                    .setHeader("SAP_MessageProcessingLogID", "msg-001")
                    .setHeader("CorrelationId", "corr-001")
                    .setBody("{\"accepted\":true}"));
            inboundServer.start();
            monitoringServer.start();

            CpiTestConfig config = new CpiTestConfig(
                    monitoringServer.url("/").toString(),
                    "monitoring-user",
                    "monitoring-password",
                    inboundServer.url("/").toString(),
                    "inbound-user",
                    "inbound-password",
                    "http://collector",
                    1,
                    50);
            CpiCommunicationClient client = new CpiCommunicationClient(
                    new OkHttpTransport(),
                    config,
                    new CpiMonitoringClient(new OkHttpTransport(), config));

            CpiSendResult response = client.sendRawRequest("/iflow/orders", "<order/>");

            assertTrue(response.isSuccessful());
            assertEquals("msg-001", response.requireMessageId());
            assertEquals("corr-001", response.requireCorrelationId());
            assertEquals("/iflow/orders", inboundServer.takeRequest().getPath());
            assertEquals("application/xml; charset=utf-8", inboundServer.takeRequest().getHeader("Content-Type"));
        }
    }

    @Test
    void sendFileRequestLoadsPayloadFromFileSystem() throws Exception {
        try (MockWebServer inboundServer = new MockWebServer();
                MockWebServer monitoringServer = new MockWebServer()) {
            inboundServer.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"messageId\":\"msg-002\",\"CorrelationId\":\"corr-002\"}"));
            inboundServer.start();
            monitoringServer.start();

            Path payloadFile = Files.createTempFile("cpi-request-", ".xml");
            Files.writeString(payloadFile, "<invoice/>");

            CpiTestConfig config = new CpiTestConfig(
                    monitoringServer.url("/").toString(),
                    "monitoring-user",
                    "monitoring-password",
                    inboundServer.url("/").toString(),
                    "inbound-user",
                    "inbound-password",
                    "http://collector",
                    1,
                    50);
            CpiCommunicationClient client = new CpiCommunicationClient(
                    new OkHttpTransport(),
                    config,
                    new CpiMonitoringClient(new OkHttpTransport(), config));

            CpiSendResult response = client.sendFileRequest("iflow/invoices", payloadFile.toString());

            assertEquals("msg-002", response.requireMessageId());
            assertEquals("/iflow/invoices", inboundServer.takeRequest().getPath());
            assertEquals("<invoice/>", inboundServer.takeRequest().getBody().readUtf8());
        }
    }

    @Test
    void waitForCompletionForMessageIdResolvesCorrelationIdAndWaitsForTerminalStates() {
        RecordingMonitoringClient monitoringClient = new RecordingMonitoringClient();
        CpiTestConfig config = new CpiTestConfig(
                "https://monitoring.example.test",
                "monitoring-user",
                "monitoring-password",
                "https://inbound.example.test",
                "inbound-user",
                "inbound-password",
                "http://collector",
                1,
                50);
        CpiCommunicationClient client = new CpiCommunicationClient(new OkHttpTransport(), config, monitoringClient);

        List<MonitoringEntry> entries = client.waitForCompletionForMessageId("msg-003", "run-1");

        assertEquals("msg-003", monitoringClient.lastMessageId);
        assertEquals("corr-003", monitoringClient.lastCorrelationId);
        assertEquals(1, entries.size());
        assertEquals("COMPLETED", entries.get(0).status());
    }

    private static final class RecordingMonitoringClient extends CpiMonitoringClient {
        private String lastMessageId;
        private String lastCorrelationId;

        private RecordingMonitoringClient() {
            super(
                    new OkHttpTransport(),
                    new CpiTestConfig(
                            "https://monitoring.example.test",
                            "user",
                            "password",
                            "http://collector",
                            1,
                            50));
        }

        @Override
        public String getCorrelationIdForMessageId(String messageId, String runId) {
            this.lastMessageId = messageId;
            return "corr-003";
        }

        @Override
        public List<MonitoringEntry> awaitCompletion(String correlationId, String runId) {
            this.lastCorrelationId = correlationId;
            return List.of(new MonitoringEntry("msg-003", correlationId, "COMPLETED"));
        }
    }
}
