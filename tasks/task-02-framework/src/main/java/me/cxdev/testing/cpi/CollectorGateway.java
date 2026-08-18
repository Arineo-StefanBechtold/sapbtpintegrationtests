package me.cxdev.testing.cpi;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface CollectorGateway {
    List<Document> fetchDocuments(String runId, String messageId) throws IOException, InterruptedException;

    Set<String> listMessageIds(String runId) throws IOException, InterruptedException;

    ResidualState fetchResidualState(String runId) throws IOException, InterruptedException;

    void releaseMessageGroup(String runId, String messageId) throws IOException, InterruptedException;
}
