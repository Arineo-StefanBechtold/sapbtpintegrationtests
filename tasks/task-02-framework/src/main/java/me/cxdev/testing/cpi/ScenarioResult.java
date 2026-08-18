package me.cxdev.testing.cpi;

import java.util.List;
import java.util.Map;

public record ScenarioResult(String testCaseName, String runId, String correlationId, List<String> messageIds,
                             Map<String, List<Document>> documentsByMessageId) {
}
