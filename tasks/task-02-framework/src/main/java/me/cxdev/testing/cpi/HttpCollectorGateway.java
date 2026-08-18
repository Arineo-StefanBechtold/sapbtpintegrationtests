package me.cxdev.testing.cpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpCollectorGateway implements CollectorGateway {
    private final HttpClient httpClient;
    private final URI baseUri;
    private final ObjectMapper objectMapper;

    public HttpCollectorGateway(URI baseUri) {
        this(HttpClient.newHttpClient(), baseUri, new ObjectMapper());
    }

    public HttpCollectorGateway(HttpClient httpClient, URI baseUri, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Document> fetchDocuments(String runId, String messageId) throws IOException, InterruptedException {
        HttpResponse<String> metadata = send(HttpRequest.newBuilder(baseUri.resolve("/runs/" + runId + "/messages/" + messageId)).GET().build(), HttpResponse.BodyHandlers.ofString());
        ensureStatus(metadata, 200, runId, List.of(messageId), "collector:/runs/{runId}/messages/{messageId}");
        JsonNode node = objectMapper.readTree(metadata.body());
        List<Document> documents = new ArrayList<>();
        for (JsonNode documentNode : node.path("documents")) {
            int sequenceNumber = documentNode.path("sequenceNumber").asInt(-1);
            String contentType = documentNode.path("contentType").asText();
            if (sequenceNumber < 1 || contentType.isBlank()) {
                throw new DiagnosticException("Collector metadata missing required fields", runId, "n/a", List.of(messageId), "collector:/runs/{runId}/messages/{messageId}");
            }
            HttpResponse<String> payload = send(HttpRequest.newBuilder(baseUri.resolve("/runs/" + runId + "/messages/" + messageId + "/payload?sequenceNumber=" + sequenceNumber)).GET().build(), HttpResponse.BodyHandlers.ofString());
            ensureStatus(payload, 200, runId, List.of(messageId), "collector:/runs/{runId}/messages/{messageId}/payload");
            if (!payload.headers().firstValue("content-type").orElse("").contains(contentType)) {
                throw new DiagnosticException("Unexpected payload content type: expected=" + contentType + ", actual=" + payload.headers().firstValue("content-type").orElse(""), runId, "n/a", List.of(messageId), "collector:/runs/{runId}/messages/{messageId}/payload");
            }
            HttpResponse<String> header = send(HttpRequest.newBuilder(baseUri.resolve("/runs/" + runId + "/messages/" + messageId + "/header?sequenceNumber=" + sequenceNumber)).GET().build(), HttpResponse.BodyHandlers.ofString());
            ensureStatus(header, 200, runId, List.of(messageId), "collector:/runs/{runId}/messages/{messageId}/header");
            if (!header.headers().firstValue("content-type").orElse("").contains("application/json")) {
                throw new DiagnosticException("Unexpected header content type: expected=application/json, actual=" + header.headers().firstValue("content-type").orElse(""), runId, "n/a", List.of(messageId), "collector:/runs/{runId}/messages/{messageId}/header");
            }
            Map<String, String> headers = new LinkedHashMap<>();
            objectMapper.readTree(header.body()).fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
            documents.add(new Document(sequenceNumber, contentType, payload.body(), headers));
        }
        return documents;
    }

    @Override
    public Set<String> listMessageIds(String runId) throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(baseUri.resolve("/runs/" + runId + "/messages")).GET().build(), HttpResponse.BodyHandlers.ofString());
        ensureStatus(response, 200, runId, List.of(), "collector:/runs/{runId}/messages");
        JsonNode node = objectMapper.readTree(response.body());
        Set<String> messageIds = new LinkedHashSet<>();
        for (JsonNode messageNode : node.path("messages")) {
            messageIds.add(messageNode.path("messageId").asText());
        }
        return messageIds;
    }

    @Override
    public ResidualState fetchResidualState(String runId) throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(baseUri.resolve("/runs/" + runId + "/residual")).GET().build(), HttpResponse.BodyHandlers.ofString());
        ensureStatus(response, 200, runId, List.of(), "collector:/runs/{runId}/residual");
        JsonNode node = objectMapper.readTree(response.body());
        List<ResidualMessage> residualMessages = new ArrayList<>();
        for (JsonNode messageNode : node.path("residualMessages")) {
            List<Integer> sequenceNumbers = new ArrayList<>();
            for (JsonNode value : messageNode.path("sequenceNumbers")) {
                sequenceNumbers.add(value.asInt());
            }
            residualMessages.add(new ResidualMessage(messageNode.path("messageId").asText(), messageNode.path("documentCount").asInt(), sequenceNumbers));
        }
        List<String> released = new ArrayList<>();
        for (JsonNode value : node.path("releasedMessageIds")) {
            released.add(value.asText());
        }
        return new ResidualState(node.path("runId").asText(), residualMessages, released);
    }

    @Override
    public void releaseMessageGroup(String runId, String messageId) throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(baseUri.resolve("/runs/" + runId + "/messages/" + messageId + "/release")).DELETE().build(), HttpResponse.BodyHandlers.ofString());
        ensureStatus(response, 204, runId, List.of(messageId), "collector:/runs/{runId}/messages/{messageId}/release");
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        return httpClient.send(request, bodyHandler);
    }

    private static void ensureStatus(HttpResponse<?> response, int expectedStatus, String runId, List<String> messageIds, String contractPart) {
        if (response.statusCode() != expectedStatus) {
            throw new DiagnosticException("Unexpected HTTP status: expected=" + expectedStatus + ", actual=" + response.statusCode(), runId, "n/a", messageIds, contractPart);
        }
    }
}
