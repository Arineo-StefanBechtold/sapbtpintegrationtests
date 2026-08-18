package me.cxdev.testing.cpi.collector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.cxdev.testing.cpi.http.HttpResponse;
import me.cxdev.testing.cpi.http.HttpTransport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class CollectorClient {
    private final HttpTransport transport;
    private final String collectorBaseUrl;

    public CollectorClient(HttpTransport transport, String collectorBaseUrl) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.collectorBaseUrl = normalizeBaseUrl(collectorBaseUrl);
    }

    public List<CollectedDocument> fetchDocuments(List<String> messageIds) {
        List<CollectedDocument> documents = new ArrayList<>();
        for (String messageId : messageIds) {
            HttpResponse payloadResponse = transport.get(collectorBaseUrl + "/documents/" + messageId, Map.of("Accept", "application/json, text/plain"));
            HttpResponse headersResponse = transport.get(collectorBaseUrl + "/documents/" + messageId + "/headers", Map.of("Accept", "application/json, text/plain"));
            ensureSuccess(payloadResponse, messageId, "payload");
            ensureSuccess(headersResponse, messageId, "headers");

            List<PayloadEntry> payloads = parsePayloads(payloadResponse.body());
            List<Map<String, String>> headers = parseHeaders(headersResponse.body());
            for (int i = 0; i < payloads.size(); i++) {
                PayloadEntry entry = payloads.get(i);
                Map<String, String> headerMap = headers.isEmpty() ? Map.of() : headers.get(Math.min(i, headers.size() - 1));
                documents.add(new CollectedDocument(messageId, entry.sequence(), entry.payload(), headerMap));
            }
        }
        return documents;
    }

    private void ensureSuccess(HttpResponse response, String messageId, String resource) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IllegalStateException(
                "Collector " + resource + " request failed for messageId=" + messageId + " with status " + response.statusCode());
    }

    private List<PayloadEntry> parsePayloads(String body) {
        if (body == null || body.isBlank()) {
            return List.of(new PayloadEntry(0, ""));
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return List.of(new PayloadEntry(0, body));
        }
        try {
            Object token = new JSONTokener(body).nextValue();
            if (token instanceof JSONObject jsonObject) {
                JSONArray documents = jsonObject.optJSONArray("documents");
                if (documents != null) {
                    return payloadEntriesFromArray(documents);
                }
                if (jsonObject.has("payload")) {
                    return List.of(new PayloadEntry(jsonObject.optInt("sequence", 0), jsonObject.optString("payload", "")));
                }
            }
            if (token instanceof JSONArray array) {
                return payloadEntriesFromArray(array);
            }
        } catch (Exception ignored) {
            return List.of(new PayloadEntry(0, body));
        }
        return List.of(new PayloadEntry(0, body));
    }

    private List<PayloadEntry> payloadEntriesFromArray(JSONArray array) {
        List<PayloadEntry> entries = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject jsonObject) {
                entries.add(new PayloadEntry(jsonObject.optInt("sequence", i), jsonObject.optString("payload", jsonObject.toString())));
            } else {
                entries.add(new PayloadEntry(i, String.valueOf(item)));
            }
        }
        return entries;
    }

    private List<Map<String, String>> parseHeaders(String body) {
        if (body == null || body.isBlank()) {
            return List.of(Map.of());
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return List.of(parsePlainHeaders(body));
        }
        try {
            Object token = new JSONTokener(body).nextValue();
            if (token instanceof JSONObject jsonObject) {
                JSONArray documents = jsonObject.optJSONArray("documents");
                if (documents != null) {
                    return headerEntriesFromArray(documents);
                }
                return List.of(jsonObjectToMap(jsonObject));
            }
            if (token instanceof JSONArray array) {
                return headerEntriesFromArray(array);
            }
        } catch (Exception ignored) {
            return List.of(parsePlainHeaders(body));
        }
        return List.of(parsePlainHeaders(body));
    }

    private List<Map<String, String>> headerEntriesFromArray(JSONArray array) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject jsonObject) {
                entries.add(jsonObjectToMap(jsonObject));
            }
        }
        return entries;
    }

    private Map<String, String> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (java.util.Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            headers.put(key, String.valueOf(jsonObject.opt(key)));
        }
        return headers;
    }

    private Map<String, String> parsePlainHeaders(String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = body.split("\\R");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                headers.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        return headers;
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record PayloadEntry(int sequence, String payload) {
    }
}
