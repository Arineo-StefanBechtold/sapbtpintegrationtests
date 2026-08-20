package me.cxdev.sapbtp.testing.comparison;

import java.util.List;

public interface DocumentComparator {
    ComparisonResult compare(String expected, String actual, List<String> ignoreFields);
}
