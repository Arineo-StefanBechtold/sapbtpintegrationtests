package me.cxdev.testing.cpi.suite;

import java.util.List;

public final class SuiteTemplateFixtures {
    private SuiteTemplateFixtures() {
    }

    public static SuiteScenarioDefinition orderCreated() {
        return new SuiteScenarioDefinition(
            "order-created",
            "corr-order-created",
            List.of("MSG-ORDER-CREATED-1"),
            "testcases/order-created/input.json",
            List.of(new SuiteDocumentExpectation(
                "testcases/order-created/golden-master/expected-1.xml",
                "testcases/order-created/golden-master/expected-1-header.json"
            ))
        );
    }

    public static SuiteScenarioDefinition orderCancelled() {
        return new SuiteScenarioDefinition(
            "order-cancelled",
            "corr-order-cancelled",
            List.of("MSG-ORDER-CANCELLED-1"),
            "testcases/order-cancelled/input.json",
            List.of(
                new SuiteDocumentExpectation(
                    "testcases/order-cancelled/golden-master/expected-1.xml",
                    "testcases/order-cancelled/golden-master/expected-1-header.json"
                ),
                new SuiteDocumentExpectation(
                    "testcases/order-cancelled/golden-master/expected-2.xml",
                    "testcases/order-cancelled/golden-master/expected-2-header.json"
                )
            )
        );
    }
}
