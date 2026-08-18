package me.cxdev.testing.cpi;

import java.time.Duration;

public record CpiTestConfig(String runId, Duration timeout, Duration pollingInterval) {
    public static CpiTestConfig forRun(String runId) {
        return new CpiTestConfig(runId, Duration.ofSeconds(5), Duration.ofMillis(100));
    }
}
