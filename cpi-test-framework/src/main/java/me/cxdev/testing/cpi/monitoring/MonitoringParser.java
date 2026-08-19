package me.cxdev.testing.cpi.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class MonitoringParser {
    public MonitoringResponse parse(String body) {
        try {
            JSONObject json = new JSONObject(body == null ? "{}" : body);
            JSONObject d = json.optJSONObject("d");
            if (d == null) {
                return new MonitoringResponse(List.of());
            }
            JSONArray results = d.optJSONArray("results");
            if (results == null) {
                return new MonitoringResponse(List.of());
            }
            List<MonitoringEntry> entries = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String messageGuid = firstNonBlank(
                        item.optString("MessageGuid", "").trim(),
                        item.optString("MessageId", "").trim());
                if (!messageGuid.isEmpty()) {
                    entries.add(new MonitoringEntry(
                            messageGuid,
                            firstNonBlank(item.optString("CorrelationId", "").trim(), item.optString("CorrelationID", "").trim()),
                            item.optString("Status", "").trim()));
                }
            }
            return new MonitoringResponse(entries);
        } catch (Exception e) {
            return new MonitoringResponse(List.of());
        }
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }
}
