package me.cxdev.sapbtp.testing.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import me.cxdev.sapbtp.testing.http.OkHttpTransport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class CollectorClientTest {
    @Test
    void fetchDocumentsLoadsPayloadAndHeadersForEachMessageId() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("[{\"sequence\":0,\"payload\":\"<order/>\"},{\"sequence\":1,\"payload\":\"<invoice/>\"}]"));
            server.enqueue(new MockResponse().setBody("[{\"CorrelationId\":\"corr-1\",\"Content-Type\":\"application/xml\"},{\"CorrelationId\":\"corr-1\",\"Content-Type\":\"application/xml\"}]"));
            server.start();

            CollectorClient client = new CollectorClient(new OkHttpTransport(), server.url("/").toString());

            List<CollectedDocument> documents = client.fetchDocuments(List.of("msg-001"));

            assertEquals(2, documents.size());
            assertEquals("<order/>", documents.get(0).payload());
            assertEquals("application/xml", documents.get(1).headers().get("Content-Type"));
            assertEquals("/documents/msg-001", server.takeRequest().getPath());
            assertEquals("/documents/msg-001/headers", server.takeRequest().getPath());
        }
    }
}
