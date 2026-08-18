package me.cxdev.testing.cpi.suite;

import me.cxdev.testing.cpi.CollectorGateway;
import me.cxdev.testing.cpi.CpiTestConfig;
import me.cxdev.testing.cpi.CpiTestFramework;
import me.cxdev.testing.cpi.Document;
import me.cxdev.testing.cpi.MonitoringRecord;
import me.cxdev.testing.cpi.ResidualMessage;
import me.cxdev.testing.cpi.ResidualState;
import me.cxdev.testing.cpi.ScenarioResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SuiteTemplateExampleTest {
    @Test
    void orderCreatedUsesFrameworkDslAndBusinessAssertion() throws Exception {
        SuiteScenarioDefinition scenario = SuiteTemplateFixtures.orderCreated();
        CpiTestFramework framework = new CpiTestFramework(
            CpiTestConfig.forRun("suite-run"),
            correlationId -> List.of(new MonitoringRecord(correlationId, scenario.messageIds().get(0), "COMPLETED")),
            new CollectorGateway() {
                @Override
                public List<Document> fetchDocuments(String runId, String messageId) {
                    return List.of(new Document(1, "application/xml", "<Order><Status>POSTED</Status></Order>", Map.of("content-type", "application/xml")));
                }

                @Override
                public Set<String> listMessageIds(String runId) {
                    return Set.copyOf(scenario.messageIds());
                }

                @Override
                public ResidualState fetchResidualState(String runId) {
                    return new ResidualState(runId, List.of(new ResidualMessage(scenario.messageIds().get(0), 1, List.of(1))), List.of());
                }

                @Override
                public void releaseMessageGroup(String runId, String messageId) {
                }
            }
        );

        ScenarioResult result = framework.scenario(scenario.name())
            .withCorrelationId(scenario.correlationId())
            .expectMessageIds(scenario.messageIds())
            .verify();

        assertTrue(result.documentsByMessageId().get(scenario.messageIds().get(0)).get(0).payload().contains("<Status>POSTED</Status>"));
    }
}
