package me.cxdev.testing.cpi.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CollectorContractVerifier {
    private final ObjectMapper objectMapper = new ObjectMapper();

    void verify(Path openApiPath, URI collectorBaseUri, String runId, String messageId) throws Exception {
        Map<String, Object> openApi = loadOpenApi(openApiPath);
        Map<String, Object> paths = cast(openApi.get("paths"));
        require(paths.containsKey("/collect"), "Missing /collect contract");
        require(paths.containsKey("/runs/{runId}/messages/{messageId}"), "Missing message contract");

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> collectResponse = client.send(HttpRequest.newBuilder(collectorBaseUri.resolve("/collect"))
            .header("content-type", "application/xml")
            .header("x-test-run-id", runId)
            .header("x-message-id", messageId)
            .POST(HttpRequest.BodyPublishers.ofString("<contract-check />"))
            .build(), HttpResponse.BodyHandlers.ofString());
        require(collectResponse.statusCode() == 202, "collect returned wrong status: " + collectResponse.statusCode());
        JsonNode collectJson = objectMapper.readTree(collectResponse.body());
        require(collectJson.hasNonNull("messageId"), "collect response missing required messageId");
        require(collectJson.hasNonNull("sequenceNumber"), "collect response missing required sequenceNumber");

        HttpResponse<String> metadata = client.send(HttpRequest.newBuilder(collectorBaseUri.resolve("/runs/" + runId + "/messages/" + messageId)).GET().build(), HttpResponse.BodyHandlers.ofString());
        require(metadata.statusCode() == 200, "message metadata returned wrong status: " + metadata.statusCode());
        JsonNode metadataJson = objectMapper.readTree(metadata.body());
        require(metadataJson.hasNonNull("documents"), "message metadata missing required documents");

        HttpResponse<String> header = client.send(HttpRequest.newBuilder(collectorBaseUri.resolve("/runs/" + runId + "/messages/" + messageId + "/header?sequenceNumber=1")).GET().build(), HttpResponse.BodyHandlers.ofString());
        require(header.statusCode() == 200, "header returned wrong status: " + header.statusCode());
        require(header.headers().firstValue("content-type").orElse("").contains("application/json"), "header returned wrong content-type");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static Map<String, Object> loadOpenApi(Path path) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(path)) {
            return yaml.load(inputStream);
        }
    }
}
