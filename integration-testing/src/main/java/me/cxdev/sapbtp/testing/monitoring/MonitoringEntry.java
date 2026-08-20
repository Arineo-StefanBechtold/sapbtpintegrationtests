package me.cxdev.sapbtp.testing.monitoring;

import java.util.Locale;

public record MonitoringEntry(String messageId, String correlationId, String status) {
    public boolean isTerminal() {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return "COMPLETED".equals(normalizedStatus) || "FAILED".equals(normalizedStatus);
    }
}
