package me.cxdev.testing.cpi;

import java.util.List;

public record ResidualState(String runId, List<ResidualMessage> residualMessages, List<String> releasedMessageIds) {
}
