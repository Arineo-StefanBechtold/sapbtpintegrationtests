package com.example.cpi.suite;

import me.cxdev.testing.cpi.framework.CpiSendResult;
import me.cxdev.testing.cpi.framework.CpiTestContext;
import me.cxdev.testing.cpi.framework.CpiTestDsl;
import me.cxdev.testing.cpi.framework.annotation.CpiTest;
import org.junit.jupiter.api.Test;

/**
 * Test: Order Created — simple success case.
 *
 * <p>Verifies that a new sales order sent to CPI results in exactly one outbound document
 * with status {@code POSTED}. Input and expected output are stored under
 * {@code testcases/order-created/}.
 *
 * <p><b>Adaptation notes:</b>
 * <ul>
 *   <li>Replace the iflow endpoint {@code "http/order-created"} with the actual HTTP sender address.</li>
 *   <li>Replace the input file path with the location of your test input document.</li>
 *   <li>Adjust golden-master assertions to match your integration's output headers and body.</li>
 *   <li>Update golden masters intentionally: {@code ./gradlew test -Dgoldenmaster.update=true}</li>
 * </ul>
 */
@CpiTest
class OrderCreatedTest {

    /**
     * Sends the order-created input message to CPI and asserts the outbound document.
     *
     * @param ctx injected by the framework; provides CPI send, monitoring, and collector access
     */
    @Test
    void orderCreated_resultsInPostedStatus(CpiTestContext ctx) {
        // Send the input file to the CPI iflow HTTP endpoint
        CpiSendResult sendResult = ctx.sendFileRequestToCpi(
                "http/order-created",                                          // TODO: replace with actual sender endpoint
                "src/test/resources/testcases/order-created/input.json");      // TODO: replace with actual input path

        String messageId     = sendResult.requireMessageId();
        String correlationId = ctx.getCorrelationIdForMessageId(messageId);

        // Wait until CPI processing is complete
        ctx.waitForCompletion(correlationId);

        // Assert the result via the collector
        CpiTestDsl.given(ctx)
                .correlationId(correlationId)
                .when()
                .fetchFromCollector()
                .document(0)
                .hasHeader("Content-Type", "application/xml")
                .hasStatus("POSTED");                                          // TODO: adjust expected status if needed
    }
}
