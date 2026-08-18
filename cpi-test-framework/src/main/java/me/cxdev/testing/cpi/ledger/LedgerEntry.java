package me.cxdev.testing.cpi.ledger;

import java.util.List;

public record LedgerEntry(String correlationId, String testCaseName, List<String> messageIds) {
    public LedgerEntry {
        messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
    }
}
