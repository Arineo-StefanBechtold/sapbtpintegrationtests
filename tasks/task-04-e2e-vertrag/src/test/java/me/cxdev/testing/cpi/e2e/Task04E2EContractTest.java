package me.cxdev.testing.cpi.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import me.cxdev.testing.cpi.CpiTestConfig;
import me.cxdev.testing.cpi.CpiTestFramework;
import me.cxdev.testing.cpi.DiagnosticException;
import me.cxdev.testing.cpi.Document;
import me.cxdev.testing.cpi.HttpCollectorGateway;
import me.cxdev.testing.cpi.HttpMonitoringClient;
import me.cxdev.testing.cpi.ResidualState;
import me.cxdev.testing.cpi.ScenarioResult;
import me.cxdev.testing.cpi.suite.SuiteDocumentExpectation;
import me.cxdev.testing.cpi.suite.SuiteScenarioDefinition;
import me.cxdev.testing.cpi.suite.SuiteTemplateFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.MethodName.class)
class Task04E2EContractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path REPOSITORY_ROOT = Path.of("/home/runner/work/sapbtpintegrationtests/sapbtpintegrationtests");
    private static final Path REPORTS_DIR = REPOSITORY_ROOT.resolve("tasks/task-04-e2e-vertrag/reports");
    private static final ReportCollector REPORT_COLLECTOR = new ReportCollector();

    private static WireMockServer monitoringServer;
    private static CollectorProcess collectorProcess;

    @BeforeAll
    static void setUp() throws Exception {
        Files.createDirectories(REPORTS_DIR);
        Files.deleteIfExists(REPORTS_DIR.resolve("e2e-summary.json"));
        Files.deleteIfExists(REPORTS_DIR.resolve("residual-state.json"));
        Files.deleteIfExists(REPORTS_DIR.resolve("contract-mismatches.md"));

        monitoringServer = new WireMockServer(0);
        monitoringServer.start();
        stubMonitoring("corr-order-created", "order-created.json");
        stubMonitoring("corr-order-cancelled", "order-cancelled.json");
        stubMonitoring("corr-order-created-failed", "order-created-failed.json");

        collectorProcess = CollectorProcess.launch(REPOSITORY_ROOT, Files.createTempDirectory("collector-data"));
    }

    @AfterAll
    static void tearDown() throws Exception {
        try {
            REPORT_COLLECTOR.writeReports(REPORTS_DIR);
        } finally {
            if (collectorProcess != null) {
                collectorProcess.close();
            }
            if (monitoringServer != null) {
                monitoringServer.stop();
            }
        }
    }

    @Test
    void orderCreatedRunsThroughCollectorFrameworkAndSuite() throws Exception {
        SuiteScenarioDefinition scenario = SuiteTemplateFixtures.orderCreated();
        stageCollectorDocuments("run-order-created", scenario.messageIds().get(0), scenario.expectedDocuments());
        CpiTestFramework framework = frameworkFor("run-order-created");

        ScenarioResult result = framework.scenario(scenario.name())
            .withCorrelationId(scenario.correlationId())
            .expectMessageIds(scenario.messageIds())
            .verify();

        assertScenarioDocuments(scenario, result);
        framework.releaseAll(result.messageIds());
        ResidualState residualState = new HttpCollectorGateway(collectorProcess.baseUri()).fetchResidualState("run-order-created");
        REPORT_COLLECTOR.recordSuccess(result);
        REPORT_COLLECTOR.recordResidual(residualState);
        assertEquals(List.of(), residualState.residualMessages());
    }

    @Test
    void orderCancelledSupportsMultipleDocumentsPerMessageId() throws Exception {
        SuiteScenarioDefinition scenario = SuiteTemplateFixtures.orderCancelled();
        stageCollectorDocuments("run-order-cancelled", scenario.messageIds().get(0), scenario.expectedDocuments());
        CpiTestFramework framework = frameworkFor("run-order-cancelled");

        ScenarioResult result = framework.scenario(scenario.name())
            .withCorrelationId(scenario.correlationId())
            .expectMessageIds(scenario.messageIds())
            .verify();

        assertScenarioDocuments(scenario, result);
        REPORT_COLLECTOR.recordSuccess(result);
        assertEquals(2, result.documentsByMessageId().get(scenario.messageIds().get(0)).size());
    }

    @Test
    void contractVerifierChecksCollectorAgainstOpenApi() throws Exception {
        new CollectorContractVerifier().verify(
            REPOSITORY_ROOT.resolve("tasks/task-01-collector/api-contract/openapi.yaml"),
            collectorProcess.baseUri(),
            "run-contract",
            "MSG-CONTRACT-1"
        );
    }


    @Test
    void collectorContractMismatchIncludesExpectedAndActualValues() throws Exception {
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/runs/run-bad/messages/MSG-BAD", exchange -> respondJson(exchange, 200, "{\"runId\":\"run-bad\",\"messageId\":\"MSG-BAD\",\"documents\":[{\"sequenceNumber\":1,\"contentType\":\"application/xml\"}]}", "application/json"));
        server.createContext("/runs/run-bad/messages/MSG-BAD/payload", exchange -> respondJson(exchange, 200, "<bad />", "application/xml"));
        server.createContext("/runs/run-bad/messages/MSG-BAD/header", exchange -> respondJson(exchange, 200, "not-json", "text/plain"));
        server.start();
        try {
            DiagnosticException error = assertThrows(DiagnosticException.class,
                () -> new HttpCollectorGateway(URI.create("http://127.0.0.1:" + server.getAddress().getPort())).fetchDocuments("run-bad", "MSG-BAD"));
            REPORT_COLLECTOR.recordExpectedFailure("collector-contract-mismatch", error);
            REPORT_COLLECTOR.recordContractMismatch(error);
            assertEquals("run-bad", error.runId());
            assertEquals(List.of("MSG-BAD"), error.messageIds());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void residualStateAndDiagnosticsAreReportedForUnexpectedDocuments() throws Exception {
        stageCollectorDocuments("run-residual", "MSG-EXPECTED", List.of(SuiteTemplateFixtures.orderCreated().expectedDocuments().get(0)));
        stageCollectorDocuments("run-residual", "MSG-UNEXPECTED", List.of(SuiteTemplateFixtures.orderCreated().expectedDocuments().get(0)));
        CpiTestFramework framework = frameworkFor("run-residual");

        DiagnosticException error = assertThrows(DiagnosticException.class,
            () -> framework.assertNoResidualDocuments("corr-order-created", List.of("MSG-EXPECTED")));

        REPORT_COLLECTOR.recordExpectedFailure("residual-state", error);
        REPORT_COLLECTOR.recordResidual(new HttpCollectorGateway(collectorProcess.baseUri()).fetchResidualState("run-residual"));
    }

    @Test
    void failedMonitoringScenarioContainsMinimumDiagnosticData() throws Exception {
        CpiTestFramework framework = frameworkFor("run-failed");

        DiagnosticException error = assertThrows(DiagnosticException.class,
            () -> framework.scenario("monitoring-failure")
                .withCorrelationId("corr-order-created-failed")
                .expectMessageIds(List.of("MSG-ORDER-CREATED-FAIL"))
                .verify());

        REPORT_COLLECTOR.recordExpectedFailure("monitoring-failure", error);
        assertEquals("run-failed", error.runId());
        assertEquals("corr-order-created-failed", error.correlationId());
        assertEquals(List.of("MSG-ORDER-CREATED-FAIL"), error.messageIds());
    }

    private static CpiTestFramework frameworkFor(String runId) {
        return new CpiTestFramework(
            CpiTestConfig.forRun(runId),
            new HttpMonitoringClient(URI.create(monitoringServer.baseUrl())),
            new HttpCollectorGateway(collectorProcess.baseUri())
        );
    }

    private static void stageCollectorDocuments(String runId, String messageId, List<SuiteDocumentExpectation> expectations) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (SuiteDocumentExpectation expectation : expectations) {
            String payload = readResource(expectation.payloadResource());
            String headers = readResource(expectation.headerResource());
            JsonNode headerJson = OBJECT_MAPPER.readTree(headers);
            HttpRequest request = HttpRequest.newBuilder(collectorProcess.baseUri().resolve("/collect"))
                .header("content-type", headerJson.path("content-type").asText("application/xml"))
                .header("x-test-run-id", runId)
                .header("x-message-id", messageId)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(202, response.statusCode());
        }
    }

    private static void assertScenarioDocuments(SuiteScenarioDefinition scenario, ScenarioResult result) throws IOException {
        List<Document> documents = result.documentsByMessageId().get(scenario.messageIds().get(0));
        assertEquals(scenario.expectedDocuments().size(), documents.size());
        for (int index = 0; index < scenario.expectedDocuments().size(); index++) {
            SuiteDocumentExpectation expectation = scenario.expectedDocuments().get(index);
            Document document = documents.get(index);
            assertEquals(readResource(expectation.payloadResource()).trim(), document.payload().trim());
            JsonNode expectedHeader = OBJECT_MAPPER.readTree(readResource(expectation.headerResource()));
            assertEquals(expectedHeader.path("content-type").asText(), document.headers().get("content-type"));
        }
    }

    private static String readResource(String resourcePath) throws IOException {
        return Files.readString(REPOSITORY_ROOT.resolve("tasks/task-03-suite-template/src/main/resources").resolve(resourcePath));
    }

    private static void respondJson(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        exchange.getResponseHeaders().add("content-type", contentType);
        byte[] responseBody = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBody.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }

    private static void stubMonitoring(String correlationId, String fixtureName) throws IOException {
        monitoringServer.stubFor(get(urlPathEqualTo("/MessageProcessingLogs"))
            .withQueryParam("correlationId", equalTo(correlationId))
            .willReturn(aResponse()
                .withHeader("content-type", "application/json")
                .withBody(Files.readString(REPOSITORY_ROOT.resolve("tasks/task-04-e2e-vertrag/fixtures/monitoring").resolve(fixtureName)))));
    }
}
