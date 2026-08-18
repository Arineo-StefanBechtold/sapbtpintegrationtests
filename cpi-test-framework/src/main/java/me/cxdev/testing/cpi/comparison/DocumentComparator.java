package me.cxdev.testing.cpi.comparison;

import java.util.List;

public interface DocumentComparator {
    ComparisonResult compare(String expected, String actual, List<String> ignoreFields);
}
