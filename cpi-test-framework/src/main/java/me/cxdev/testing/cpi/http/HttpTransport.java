package me.cxdev.testing.cpi.http;

import java.util.Map;

public interface HttpTransport {
    HttpResponse get(String url, Map<String, String> headers);

    HttpResponse post(String url, String body, Map<String, String> headers);
}
