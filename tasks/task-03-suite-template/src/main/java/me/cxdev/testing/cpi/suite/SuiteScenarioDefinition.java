package me.cxdev.testing.cpi.suite;

import java.util.List;

public record SuiteScenarioDefinition(String name, String correlationId, java.util.List<String> messageIds,
                                      String inputResource, List<SuiteDocumentExpectation> expectedDocuments) {
}
