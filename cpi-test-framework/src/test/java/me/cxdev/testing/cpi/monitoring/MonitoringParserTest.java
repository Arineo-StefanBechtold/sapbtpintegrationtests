package me.cxdev.testing.cpi.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MonitoringParserTest {
    @Test
    void parsesMessageIdsFromFixture() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("fixtures/monitoring-response.json")) {
            assertNotNull(inputStream);
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            MonitoringResponse response = new MonitoringParser().parse(body);

            assertEquals(java.util.List.of("msg-001", "msg-002"), response.messageIds());
        }
    }
}
