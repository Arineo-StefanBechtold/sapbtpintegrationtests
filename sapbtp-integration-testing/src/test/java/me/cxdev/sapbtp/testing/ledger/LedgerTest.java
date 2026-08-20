package me.cxdev.sapbtp.testing.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class LedgerTest {
    @Test
    void registerAndLookupWorks() {
        Ledger ledger = new Ledger();
        ledger.register("corr-1", "test-case-1");
        ledger.recordMessageIds("corr-1", List.of("msg-1", "msg-2"));

        LedgerEntry entry = ledger.lookup("corr-1");

        assertNotNull(entry);
        assertEquals("test-case-1", entry.testCaseName());
        assertEquals(List.of("msg-1", "msg-2"), entry.messageIds());
    }

    @Test
    void concurrentRegistrationIsThreadSafe() throws InterruptedException, ExecutionException {
        Ledger ledger = new Ledger();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int index = i;
                tasks.add(() -> {
                    ledger.register("corr-" + index, "test-" + index);
                    return null;
                });
            }
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(100, ledger.getAll().size());
    }
}
