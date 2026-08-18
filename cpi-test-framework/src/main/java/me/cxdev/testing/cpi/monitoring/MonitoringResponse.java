package me.cxdev.testing.cpi.monitoring;

import java.util.List;

public record MonitoringResponse(List<String> messageIds) {
    public MonitoringResponse {
        messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
    }
}
