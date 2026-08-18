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
            List<String> messageIds = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String messageGuid = item.optString("MessageGuid", "").trim();
                if (!messageGuid.isEmpty()) {
                    messageIds.add(messageGuid);
                }
            }
            return new MonitoringResponse(messageIds);
        } catch (Exception e) {
            return new MonitoringResponse(List.of());
        }
    }
}
