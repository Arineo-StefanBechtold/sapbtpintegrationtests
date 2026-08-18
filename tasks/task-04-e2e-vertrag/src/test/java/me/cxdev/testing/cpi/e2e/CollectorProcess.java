package me.cxdev.testing.cpi.e2e;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

final class CollectorProcess implements AutoCloseable {
    private final Process process;
    private final URI baseUri;

    private CollectorProcess(Process process, URI baseUri) {
        this.process = process;
        this.baseUri = baseUri;
    }

    static CollectorProcess launch(Path repositoryRoot, Path dataDir) throws Exception {
        int port = freePort();
        Path collectorDir = repositoryRoot.resolve("tasks/task-01-collector");
        ProcessBuilder builder = new ProcessBuilder("node", "src/server.js");
        builder.directory(collectorDir.toFile());
        builder.redirectErrorStream(true);
        builder.inheritIO();
        builder.environment().put("PORT", Integer.toString(port));
        builder.environment().put("HOST", "127.0.0.1");
        builder.environment().put("COLLECTOR_DATA_DIR", dataDir.toString());
        Process process = builder.start();
        URI baseUri = URI.create("http://127.0.0.1:" + port);
        waitUntilHealthy(baseUri);
        return new CollectorProcess(process, baseUri);
    }

    URI baseUri() {
        return baseUri;
    }

    private static void waitUntilHealthy(URI baseUri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            try {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(baseUri.resolve("/health")).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException ignored) {
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Collector did not become healthy");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Override
    public void close() {
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
