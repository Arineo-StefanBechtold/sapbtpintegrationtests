package me.cxdev.testing.cpi;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpiTestFrameworkTest {
    @Test
    void verifiesScenarioThroughDsl() throws Exception {
        CpiTestFramework framework = new CpiTestFramework(
            CpiTestConfig.forRun("run-1"),
            correlationId -> List.of(new MonitoringRecord(correlationId, "MSG-1", "COMPLETED")),
            new CollectorGateway() {
                @Override
                public List<Document> fetchDocuments(String runId, String messageId) {
                    return List.of(new Document(1, "application/xml", "<status>POSTED</status>", Map.of("content-type", "application/xml")));
                }

                @Override
                public Set<String> listMessageIds(String runId) {
                    return Set.of("MSG-1");
                }

                @Override
                public ResidualState fetchResidualState(String runId) {
                    return new ResidualState(runId, List.of(new ResidualMessage("MSG-1", 1, List.of(1))), List.of());
                }

                @Override
                public void releaseMessageGroup(String runId, String messageId) {
                }
            }
        );

        ScenarioResult result = framework.scenario("order-created")
            .withCorrelationId("corr-1")
            .expectMessageIds(List.of("MSG-1"))
            .verify();

        assertEquals("<status>POSTED</status>", result.documentsByMessageId().get("MSG-1").get(0).payload());
    }

    @Test
    void reportsResidualDocumentsWithDiagnosticContext() {
        CpiTestFramework framework = new CpiTestFramework(
            CpiTestConfig.forRun("run-1"),
            correlationId -> List.of(new MonitoringRecord(correlationId, "MSG-1", "COMPLETED")),
            new CollectorGateway() {
                @Override
                public List<Document> fetchDocuments(String runId, String messageId) {
                    return List.of();
                }

                @Override
                public Set<String> listMessageIds(String runId) {
                    return Set.of("MSG-1", "MSG-EXTRA");
                }

                @Override
                public ResidualState fetchResidualState(String runId) {
                    return new ResidualState(runId, List.of(new ResidualMessage("MSG-EXTRA", 1, List.of(1))), List.of("MSG-1"));
                }

                @Override
                public void releaseMessageGroup(String runId, String messageId) {
                }
            }
        );

        DiagnosticException error = assertThrows(DiagnosticException.class,
            () -> framework.assertNoResidualDocuments("corr-1", List.of("MSG-1")));
        assertEquals("run-1", error.runId());
    }
}
