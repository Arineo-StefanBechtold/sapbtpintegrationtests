package com.example.cpi.suite;

import me.cxdev.testing.cpi.framework.CpiIntegrationTest;
import me.cxdev.testing.cpi.framework.GoldenMaster;
import me.cxdev.testing.cpi.framework.TestCase;
import me.cxdev.testing.cpi.framework.annotation.CpiTest;
import org.junit.jupiter.api.Test;

/**
 * Test: Order Cancelled — multi-step scenario.
 *
 * <p>Verifies that an order cancellation triggers the expected processing sequence.
 * The test covers a scenario with multiple processing steps (sequence length &gt; 1)
 * and checks that the final state is {@code CANCELLED}.
 *
 * <p><b>Adaptation notes:</b>
 * <ul>
 *   <li>Replace the iflow ID with the cancellation iflow of your project.</li>
 *   <li>Adjust {@code expectDocumentCount} if your integration produces a different number of documents.</li>
 *   <li>Update golden masters intentionally: {@code ./gradlew test -Dgoldenmaster.update=true}</li>
 * </ul>
 */
@CpiTest
class OrderCancelledTest {

    /**
     * Sends a cancellation message and verifies the two-step processing result.
     */
    @Test
    void orderCancelled_resultsInCancelledStatus_afterTwoSteps() {
        TestCase testCase = TestCase.load("testcases/order-cancelled");

        CpiIntegrationTest.builder()
                .withIflow("OrderCancelled_iflow")             // TODO: replace with actual iflow name
                .withInput(testCase.getInput())
                .expectDocumentCount(1)
                .expectStatus("CANCELLED")
                .expectProcessingSteps(2)
                .assertOutputMatches(GoldenMaster.from(testCase))
                .run();
    }
}
