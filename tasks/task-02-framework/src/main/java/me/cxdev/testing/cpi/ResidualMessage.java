package me.cxdev.testing.cpi;

import java.util.List;

public record ResidualMessage(String messageId, int documentCount, List<Integer> sequenceNumbers) {
}
