package com.example.cpi.suite;

import me.cxdev.testing.cpi.framework.CpiIntegrationTest;
import me.cxdev.testing.cpi.framework.GoldenMaster;
import me.cxdev.testing.cpi.framework.TestCase;
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
 *   <li>Replace the iflow ID {@code "OrderCreated_iflow"} with the actual integration flow name.</li>
 *   <li>Adjust the expected status in the golden master if your integration uses a different value.</li>
 *   <li>Update golden masters intentionally: {@code ./gradlew test -Dgoldenmaster.update=true}</li>
 * </ul>
 */
@CpiTest
class OrderCreatedTest {

    /**
     * Sends the order-created input message to CPI and compares the result against the golden master.
     */
    @Test
    void orderCreated_resultsInPostedStatus() {
        TestCase testCase = TestCase.load("testcases/order-created");

        CpiIntegrationTest.builder()
                .withIflow("OrderCreated_iflow")              // TODO: replace with actual iflow name
                .withInput(testCase.getInput())
                .expectDocumentCount(1)
                .expectStatus("POSTED")
                .assertOutputMatches(GoldenMaster.from(testCase))
                .run();
    }
}
