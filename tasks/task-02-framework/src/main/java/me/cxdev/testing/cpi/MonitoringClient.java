package me.cxdev.testing.cpi;

import java.io.IOException;
import java.util.List;

public interface MonitoringClient {
    List<MonitoringRecord> fetchByCorrelationId(String correlationId) throws IOException, InterruptedException;
}
