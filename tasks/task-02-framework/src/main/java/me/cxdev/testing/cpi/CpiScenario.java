package me.cxdev.testing.cpi;

import java.util.List;

public final class CpiScenario {
    private final CpiTestFramework framework;
    private final String testCaseName;
    private String correlationId;
    private List<String> expectedMessageIds = List.of();

    CpiScenario(CpiTestFramework framework, String testCaseName) {
        this.framework = framework;
        this.testCaseName = testCaseName;
    }

    public CpiScenario withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public CpiScenario expectMessageIds(List<String> expectedMessageIds) {
        this.expectedMessageIds = List.copyOf(expectedMessageIds);
        return this;
    }

    public ScenarioResult verify() throws Exception {
        return framework.verify(testCaseName, correlationId, expectedMessageIds);
    }
}
