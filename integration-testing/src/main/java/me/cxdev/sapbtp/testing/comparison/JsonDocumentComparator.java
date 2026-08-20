package me.cxdev.sapbtp.testing.comparison;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

public class JsonDocumentComparator implements DocumentComparator {
    @Override
    public ComparisonResult compare(String expected, String actual, List<String> ignoreFields) {
        try {
            Object expectedJson = new JSONTokener(expected).nextValue();
            Object actualJson = new JSONTokener(actual).nextValue();
            for (String ignoreField : ignoreFields == null ? List.<String>of() : ignoreFields) {
                List<PathToken> tokens = tokenize(ignoreField);
                removePath(expectedJson, tokens, 0);
                removePath(actualJson, tokens, 0);
            }
            JSONCompareResult result = JSONCompare.compareJSON(
                    toJsonString(expectedJson),
                    toJsonString(actualJson),
                    JSONCompareMode.LENIENT);
            return new ComparisonResult(result.passed(), result.getMessage());
        } catch (Exception e) {
            return new ComparisonResult(false, "JSON comparison failed: " + e.getMessage());
        }
    }

    private String toJsonString(Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject.toString();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray.toString();
        }
        return String.valueOf(value);
    }

    private void removePath(Object current, List<PathToken> tokens, int index) {
        if (current == null || index >= tokens.size()) {
            return;
        }
        PathToken token = tokens.get(index);
        if (current instanceof JSONObject jsonObject) {
            if (!jsonObject.has(token.name())) {
                return;
            }
            if (index == tokens.size() - 1 && token.arrayIndex() == null) {
                jsonObject.remove(token.name());
                return;
            }
            Object next = jsonObject.opt(token.name());
            if (token.arrayIndex() != null && next instanceof JSONArray array) {
                removeFromArray(array, token.arrayIndex(), tokens, index + 1);
                return;
            }
            removePath(next, tokens, index + 1);
        } else if (current instanceof JSONArray array && token.arrayIndex() != null) {
            removeFromArray(array, token.arrayIndex(), tokens, index + 1);
        }
    }

    private void removeFromArray(JSONArray array, int arrayIndex, List<PathToken> tokens, int nextIndex) {
        if (arrayIndex < 0 || arrayIndex >= array.length()) {
            return;
        }
        if (nextIndex >= tokens.size()) {
            array.remove(arrayIndex);
            return;
        }
        removePath(array.opt(arrayIndex), tokens, nextIndex);
    }

    private List<PathToken> tokenize(String path) {
        String sanitized = path.replace("$.", "").replace("$", "");
        String[] segments = sanitized.split("\\.");
        List<PathToken> tokens = new ArrayList<>();
        for (String segment : segments) {
            if (segment.isBlank()) {
                continue;
            }
            int bracketIndex = segment.indexOf('[');
            if (bracketIndex > -1 && segment.endsWith("]")) {
                String name = segment.substring(0, bracketIndex);
                int arrayIndex = Integer.parseInt(segment.substring(bracketIndex + 1, segment.length() - 1));
                tokens.add(new PathToken(name, arrayIndex));
            } else {
                tokens.add(new PathToken(segment, null));
            }
        }
        return tokens;
    }

    private record PathToken(String name, Integer arrayIndex) {
    }
}
