package me.cxdev.testing.cpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HttpMonitoringClient implements MonitoringClient {
    private final HttpClient httpClient;
    private final URI baseUri;
    private final ObjectMapper objectMapper;

    public HttpMonitoringClient(URI baseUri) {
        this(HttpClient.newHttpClient(), baseUri, new ObjectMapper());
    }

    public HttpMonitoringClient(HttpClient httpClient, URI baseUri, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MonitoringRecord> fetchByCorrelationId(String correlationId) throws IOException, InterruptedException {
        URI uri = baseUri.resolve("/MessageProcessingLogs?correlationId=" + URLEncoder.encode(correlationId, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        List<MonitoringRecord> records = new ArrayList<>();
        for (JsonNode node : root.path("d").path("results")) {
            records.add(new MonitoringRecord(
                node.path("correlationId").asText(),
                node.path("messageId").asText(),
                node.path("status").asText()
            ));
        }
        return records;
    }
}
