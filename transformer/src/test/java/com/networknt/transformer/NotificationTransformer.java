package com.networknt.transformer;

import com.networknt.config.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * When an organization has standard response structure defined, the light-gateway might be responsible
 * for transforming the standard structure to something legacy consumers understand.
 *
 * @author Steve Hu
 */
public class NotificationTransformer implements Transformer {
    @Override
    public String transform(String input) {
        // the source input is a JSON standard notification response. Convert to an POJO.
        Map<String, Object> sourceMap = JsonMapper.string2Map(input);
        Map<String, Object> targetMap = new LinkedHashMap<>();

        targetMap.put("status", statusConverter(404)); //  404 is the response code.
        // get the first notification from the list.
        Map<String, Object> notification = null;
        List notifications = (List)sourceMap.get("notifications");
        if(notifications != null && notifications.size() > 0) {
            notification = (Map<String, Object>)notifications.get(0);
        }
        if(notification != null) {
            targetMap.put("message", notification.get("message"));
            Map<String, Object> timestamp = new LinkedHashMap<>();
            long ts = (Long)notification.get("timestamp");
            Instant instant = Instant.ofEpochMilli(ts);
            timestamp.put("epochSecond", instant.getEpochSecond());
            timestamp.put("nano", instant.getNano());
            targetMap.put("timestamp", timestamp);
        }
        return JsonMapper.toJson(targetMap);
    }

    @Override
    public String getName() {
        return "notification";
    }

    private String statusConverter(int statusCode) {
        String result = "SUCCESS";
        switch(statusCode) {
            case 404:
                result = "NOT FOUND";
                break;
        }
        return result;
    }
}
