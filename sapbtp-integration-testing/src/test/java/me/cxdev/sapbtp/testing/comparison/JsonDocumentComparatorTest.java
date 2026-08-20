package me.cxdev.sapbtp.testing.comparison;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class JsonDocumentComparatorTest {
    private final JsonDocumentComparator comparator = new JsonDocumentComparator();

    @Test
    void identicalJsonMatches() {
        ComparisonResult result = comparator.compare("{\"id\":1,\"status\":\"ok\"}", "{\"status\":\"ok\",\"id\":1}", List.of());

        assertTrue(result.match());
    }

    @Test
    void differentJsonReturnsDiff() {
        ComparisonResult result = comparator.compare("{\"id\":1}", "{\"id\":2}", List.of());

        assertFalse(result.match());
        assertTrue(result.diff().contains("id"));
    }

    @Test
    void ignoredPathIsSkipped() {
        ComparisonResult result = comparator.compare(
                "{\"id\":1,\"timestamp\":\"abc\"}",
                "{\"id\":1,\"timestamp\":\"xyz\"}",
                List.of("$.timestamp"));

        assertTrue(result.match());
    }
}
