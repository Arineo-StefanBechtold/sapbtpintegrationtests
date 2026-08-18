package me.cxdev.testing.cpi;

import java.util.List;

public class DiagnosticException extends AssertionError {
    private final String runId;
    private final String correlationId;
    private final List<String> messageIds;
    private final String contractPart;

    public DiagnosticException(String message, String runId, String correlationId, List<String> messageIds, String contractPart) {
        super(message + " | runId=" + runId + " | correlationId=" + correlationId + " | messageIds=" + messageIds + " | contractPart=" + contractPart);
        this.runId = runId;
        this.correlationId = correlationId;
        this.messageIds = List.copyOf(messageIds);
        this.contractPart = contractPart;
    }

    public String runId() {
        return runId;
    }

    public String correlationId() {
        return correlationId;
    }

    public List<String> messageIds() {
        return messageIds;
    }

    public String contractPart() {
        return contractPart;
    }
}
