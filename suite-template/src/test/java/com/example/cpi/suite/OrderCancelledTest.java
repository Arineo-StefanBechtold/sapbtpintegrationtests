package com.example.cpi.suite;

import me.cxdev.sapbtp.testing.framework.CpiSendResult;
import me.cxdev.sapbtp.testing.framework.CpiTestContext;
import me.cxdev.sapbtp.testing.framework.CpiTestDsl;
import me.cxdev.sapbtp.testing.framework.annotation.CpiTest;
import org.junit.jupiter.api.Test;

/**
 * Test: Order Cancelled — multi-step scenario.
 *
 * <p>Verifies that an order cancellation triggers the expected two-step processing sequence
 * and results in a document with status {@code CANCELLED}.
 *
 * <p><b>Adaptation notes:</b>
 * <ul>
 *   <li>Replace the iflow endpoint with the cancellation sender address of your project.</li>
 *   <li>Adjust {@code document(0)} / {@code document(1)} if your integration produces a different document count.</li>
 *   <li>Update golden masters intentionally: {@code ./gradlew test -Dgoldenmaster.update=true}</li>
 * </ul>
 */
@CpiTest
class OrderCancelledTest {

    /**
     * Sends a cancellation message and verifies the two-step processing result.
     *
     * @param ctx injected by the framework; provides CPI send, monitoring, and collector access
     */
    @Test
    void orderCancelled_resultsInCancelledStatus_afterTwoSteps(CpiTestContext ctx) {
        // Send the cancellation input to the CPI iflow HTTP endpoint
        CpiSendResult sendResult = ctx.sendFileRequestToCpi(
                "http/order-cancelled",                                          // TODO: replace with actual sender endpoint
                "src/test/resources/testcases/order-cancelled/input.json");      // TODO: replace with actual input path

        String messageId     = sendResult.requireMessageId();
        String correlationId = ctx.getCorrelationIdForMessageId(messageId);

        // Wait until CPI has completed all processing steps
        ctx.waitForCompletion(correlationId);

        // Assert the result — two processing steps expected, check the final document
        CpiTestDsl.given(ctx)
                .correlationId(correlationId)
                .when()
                .fetchFromCollector()
                .document(0)
                .hasHeader("Content-Type", "application/xml")
                .hasStatus("CANCELLED");                                         // TODO: adjust if your status value differs
    }
}
