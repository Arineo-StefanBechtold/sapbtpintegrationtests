package me.cxdev.sapbtp.testing.dsl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import me.cxdev.sapbtp.testing.collector.CollectedDocument;
import me.cxdev.sapbtp.testing.extension.CpiTestContext;

public final class CpiTestDsl {
    private static final AtomicReference<CpiTestContext> BOUND_CONTEXT = new AtomicReference<>();

    private CpiTestDsl() {
    }

    public static void bind(CpiTestContext context) {
        BOUND_CONTEXT.set(context);
    }

    public static void clear() {
        BOUND_CONTEXT.set(null);
    }

    public static RequestStage given() {
        CpiTestContext context = BOUND_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("No CpiTestContext is bound. Register CpiTestExtension or call given(context).");
        }
        return given(context);
    }

    public static RequestStage given(CpiTestContext context) {
        return new RequestStage(context);
    }

    public static final class RequestStage {
        private final CpiTestContext context;
        private String correlationId;
        private Integer expectedDocuments;

        private RequestStage(CpiTestContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        public RequestStage correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public RequestStage messageId(String messageId) {
            this.correlationId = context.getCorrelationIdForMessageId(messageId);
            return this;
        }

        public RequestStage expectDocuments(int expectedDocuments) {
            this.expectedDocuments = expectedDocuments;
            return this;
        }

        public WhenStage when() {
            if (correlationId == null || correlationId.isBlank()) {
                throw new IllegalStateException("Correlation ID must be provided before calling when()");
            }
            return new WhenStage(context, correlationId, expectedDocuments);
        }
    }

    public static final class WhenStage {
        private final CpiTestContext context;
        private final String correlationId;
        private final Integer expectedDocuments;

        private WhenStage(CpiTestContext context, String correlationId, Integer expectedDocuments) {
            this.context = context;
            this.correlationId = correlationId;
            this.expectedDocuments = expectedDocuments;
        }

        public CpiTestResponse fetchFromCollector() {
            String testCaseName = detectTestCaseName();
            context.getLedger().register(correlationId, testCaseName);
            List<String> messageIds = context.waitForCompletion(correlationId).stream()
                    .map(me.cxdev.sapbtp.testing.monitoring.MonitoringEntry::messageId)
                    .distinct()
                    .toList();
            context.getLedger().recordMessageIds(correlationId, messageIds);
            List<CollectedDocument> documents = context.getCollectorClient().fetchDocuments(messageIds);
            context.recordDocuments(documents);
            CpiTestResponse response = new CpiTestResponse(correlationId, messageIds, documents, context.getRunId());
            if (expectedDocuments != null && response.size() != expectedDocuments) {
                throw new AssertionError("Expected " + expectedDocuments + " documents but got " + response.size()
                        + " for testCase=" + testCaseName + ", correlationId=" + correlationId
                        + ", messageIds=" + messageIds + ", runId=" + context.getRunId());
            }
            return response;
        }

        private String detectTestCaseName() {
            return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(stream -> stream
                            .filter(frame -> !frame.getClassName().startsWith(CpiTestDsl.class.getPackageName()))
                            .filter(frame -> !frame.getMethodName().startsWith("lambda$"))
                            .map(frame -> frame.getClassName() + "#" + frame.getMethodName())
                            .findFirst()
                            .orElse("unknown-test-case"));
        }
    }
}
