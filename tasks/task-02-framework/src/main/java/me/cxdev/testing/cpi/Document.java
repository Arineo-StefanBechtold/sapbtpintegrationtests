package me.cxdev.testing.cpi;

import java.util.Map;

public record Document(int sequenceNumber, String contentType, String payload, Map<String, String> headers) {
}
