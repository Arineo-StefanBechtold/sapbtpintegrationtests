package me.cxdev.testing.cpi.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.cxdev.testing.cpi.DiagnosticException;
import me.cxdev.testing.cpi.ResidualState;
import me.cxdev.testing.cpi.ScenarioResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ReportCollector {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final List<Map<String, Object>> runs = new ArrayList<>();
    private final List<Map<String, Object>> contractMismatches = new ArrayList<>();
    private final List<ResidualState> residualStates = new ArrayList<>();

    void recordSuccess(ScenarioResult result) {
        runs.add(runEntry(result.testCaseName(), result.runId(), result.correlationId(), result.messageIds(), "PASSED"));
    }

    void recordExpectedFailure(String testCase, DiagnosticException error) {
        runs.add(runEntry(testCase, error.runId(), error.correlationId(), error.messageIds(), "EXPECTED_FAILURE"));
    }

    void recordContractMismatch(DiagnosticException error) {
        Map<String, Object> mismatch = new LinkedHashMap<>();
        mismatch.put("runId", error.runId());
        mismatch.put("correlationId", error.correlationId());
        mismatch.put("messageIds", error.messageIds());
        mismatch.put("contractPart", error.contractPart());
        mismatch.put("message", error.getMessage());
        mismatch.put("timestamp", Instant.now().toString());
        contractMismatches.add(mismatch);
    }

    void recordResidual(ResidualState residualState) {
        residualStates.add(residualState);
    }

    void writeReports(Path reportsDir) throws IOException {
        Files.createDirectories(reportsDir);
        OBJECT_MAPPER.writeValue(reportsDir.resolve("e2e-summary.json").toFile(), Map.of("runs", runs));
        OBJECT_MAPPER.writeValue(reportsDir.resolve("residual-state.json").toFile(), Map.of("runs", residualStates));
        StringBuilder markdown = new StringBuilder("# Contract mismatches\n\n");
        if (contractMismatches.isEmpty()) {
            markdown.append("No contract mismatches detected.\n");
        } else {
            for (Map<String, Object> mismatch : contractMismatches) {
                markdown.append("- runId=`").append(mismatch.get("runId")).append("`, correlationId=`")
                    .append(mismatch.get("correlationId")).append("`, messageIds=`").append(mismatch.get("messageIds"))
                    .append("`, contractPart=`").append(mismatch.get("contractPart")).append("`, message=`")
                    .append(mismatch.get("message")).append("`\n");
            }
        }
        Files.writeString(reportsDir.resolve("contract-mismatches.md"), markdown.toString());
    }

    private Map<String, Object> runEntry(String testCase, String runId, String correlationId, List<String> messageIds, String status) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("testCase", testCase);
        entry.put("runId", runId);
        entry.put("correlationId", correlationId);
        entry.put("messageIds", messageIds);
        entry.put("status", status);
        entry.put("timestamp", Instant.now().toString());
        return entry;
    }
}
