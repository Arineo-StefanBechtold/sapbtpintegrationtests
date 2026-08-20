package me.cxdev.sapbtp.testing.collector;

import java.util.Map;

public record CollectedDocument(String messageId, int sequence, String payload, Map<String, String> headers) {
    public CollectedDocument {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
