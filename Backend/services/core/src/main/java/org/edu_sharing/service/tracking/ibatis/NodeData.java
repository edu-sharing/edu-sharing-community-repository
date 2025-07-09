package org.edu_sharing.service.tracking.ibatis;

import lombok.Data;
import org.edu_sharing.service.tracking.ActivityStatisticService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Data
public class NodeData {
    private String timestamp;
    private Map<String, Integer> counts = new HashMap<>();

    public void setCounts(String countsJson) {
        try {
            JSONObject object = new JSONObject(countsJson);
            for (Iterator<String> it = object.keys(); it.hasNext(); ) {
                String key = it.next();
                counts.put(key, object.getInt(key));
            }
        } catch (JSONException ignored) {}

    }
}
