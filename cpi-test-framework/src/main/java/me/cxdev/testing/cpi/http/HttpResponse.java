package me.cxdev.testing.cpi.http;

import java.util.Map;

public record HttpResponse(int statusCode, String body, Map<String, String> headers) {
    public HttpResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
