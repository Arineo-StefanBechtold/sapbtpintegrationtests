package me.cxdev.testing.cpi.residual;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.cxdev.testing.cpi.collector.CollectedDocument;
import me.cxdev.testing.cpi.ledger.Ledger;
import me.cxdev.testing.cpi.ledger.LedgerEntry;

public class ResidualChecker {
    public List<ResidualDocument> checkResiduals(Ledger ledger, List<CollectedDocument> allDocuments) {
        Map<String, LedgerEntry> entries = ledger.asMap();
        List<ResidualDocument> residuals = new ArrayList<>();
        for (CollectedDocument document : allDocuments) {
            String correlationId = findCorrelationId(document);
            LedgerEntry entry = correlationId == null ? null : entries.get(correlationId);
            if (entry == null) {
                residuals.add(new ResidualDocument(document.messageId(), correlationId, null));
                continue;
            }
            if (!entry.messageIds().isEmpty() && !entry.messageIds().contains(document.messageId())) {
                residuals.add(new ResidualDocument(document.messageId(), correlationId, entry.testCaseName()));
            }
        }
        return residuals;
    }

    private String findCorrelationId(CollectedDocument document) {
        return firstNonBlank(
                document.headers().get("CorrelationId"),
                document.headers().get("correlationId"),
                document.headers().get("X-Correlation-Id"),
                document.headers().get("SAP_MessageProcessingLogID"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
