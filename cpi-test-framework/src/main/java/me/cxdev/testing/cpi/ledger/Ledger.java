package me.cxdev.testing.cpi.ledger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Ledger {
    private final ConcurrentHashMap<String, LedgerEntry> entries = new ConcurrentHashMap<>();

    public void register(String correlationId, String testCaseName) {
        entries.compute(correlationId, (key, existing) -> existing == null
                ? new LedgerEntry(correlationId, testCaseName, List.of())
                : new LedgerEntry(correlationId, existing.testCaseName(), existing.messageIds()));
    }

    public void recordMessageIds(String correlationId, List<String> messageIds) {
        entries.compute(correlationId, (key, existing) -> existing == null
                ? new LedgerEntry(correlationId, "unknown-test-case", messageIds)
                : new LedgerEntry(existing.correlationId(), existing.testCaseName(), messageIds));
    }

    public LedgerEntry lookup(String correlationId) {
        return entries.get(correlationId);
    }

    public List<LedgerEntry> getAll() {
        return List.copyOf(entries.values());
    }

    public Map<String, LedgerEntry> asMap() {
        return Map.copyOf(entries);
    }
}
