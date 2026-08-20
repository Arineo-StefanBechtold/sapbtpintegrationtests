package me.cxdev.sapbtp.testing.comparison;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class XmlDocumentComparatorTest {
    private final XmlDocumentComparator comparator = new XmlDocumentComparator();

    @Test
    void identicalXmlMatches() {
        ComparisonResult result = comparator.compare("<order><id>1</id></order>", "<order><id>1</id></order>", List.of());

        assertTrue(result.match());
    }

    @Test
    void differentXmlReturnsDiff() {
        ComparisonResult result = comparator.compare("<order><id>1</id></order>", "<order><id>2</id></order>", List.of());

        assertFalse(result.match());
        assertTrue(result.diff().contains("Expected text value '1' but was '2'"));
    }

    @Test
    void ignoredXPathIsSkipped() {
        ComparisonResult result = comparator.compare(
                "<order><id>1</id><timestamp>abc</timestamp></order>",
                "<order><id>1</id><timestamp>xyz</timestamp></order>",
                List.of("/order/timestamp"));

        assertTrue(result.match());
    }
}
