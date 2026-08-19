package me.cxdev.testing.cpi.monitoring;

import java.util.List;
import java.util.Objects;

public record MonitoringResponse(List<MonitoringEntry> entries) {
    public MonitoringResponse {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public List<String> messageIds() {
        return entries.stream()
                .map(MonitoringEntry::messageId)
                .filter(Objects::nonNull)
                .filter(messageId -> !messageId.isBlank())
                .distinct()
                .toList();
    }

    public boolean hasEntries() {
        return !entries.isEmpty();
    }

    public boolean allEntriesTerminal() {
        return hasEntries() && entries.stream().allMatch(MonitoringEntry::isTerminal);
    }

    public String correlationIdForMessageId(String messageId) {
        return entries.stream()
                .filter(entry -> Objects.equals(messageId, entry.messageId()))
                .map(MonitoringEntry::correlationId)
                .filter(Objects::nonNull)
                .filter(correlationId -> !correlationId.isBlank())
                .findFirst()
                .orElse(null);
    }
}
