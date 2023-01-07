package org.lime.gp.extension;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.gp.lime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Modify {
    public static void modify(JsonElement base, HashMap<String, Map<String, JsonObject>> modify_map) {
        if (base.isJsonArray()) base.getAsJsonArray().forEach(item -> modify(item, modify_map));
        else if (base.isJsonObject()) {
            JsonObject _base = base.getAsJsonObject();
            _base.deepCopy().entrySet().forEach(_kv -> {
                String key = _kv.getKey();
                JsonElement item = _kv.getValue();
                if (item.isJsonObject()) modify(_base, key, item.getAsJsonObject(), modify_map);
                else modify(item, modify_map);
            });
        }
    }
    public static void modify(JsonObject base, String key, JsonObject child, HashMap<String, Map<String, JsonObject>> modify_map) {
        Map<String, JsonObject> addMap = modifyAdd(base, key, child, modify_map);
        JsonObject _base = new JsonObject();
        base.entrySet().forEach(kv -> {
            if (kv.getKey().equals(key)) addMap.forEach(_base::add);
            else _base.add(kv.getKey(), kv.getValue());
        });
        base.entrySet().clear();
        _base.entrySet().forEach(kv -> base.add(kv.getKey(), kv.getValue()));
    }
    public static Map<String, JsonObject> modifyAdd(JsonObject base, String key, JsonObject child, HashMap<String, Map<String, JsonObject>> modify_map) {
        JsonElement modify = child.remove("modify");

        for (Map.Entry<String, JsonElement> kv : child.deepCopy().entrySet()) {
            JsonElement element = kv.getValue();
            if (element.isJsonObject()) modify(child, kv.getKey(), element.getAsJsonObject(), modify_map);
            else if (element.isJsonArray()) modify(element, modify_map);
        }

        if (modify == null) return Collections.singletonMap(key, child);

        String modify_key = modify.getAsString();
        if (!modify_map.containsKey(modify_key)) {
            lime.logOP("[Warning] Modify '"+modify_key+"' in block '"+key+"' nof founded!");
            return Collections.singletonMap(key, child);
        }

        return modify_map.get(modify_key)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(kv -> key + kv.getKey(), kv -> lime.combineJson(kv.getValue().deepCopy(), child.deepCopy(), false).getAsJsonObject()));
    }
}
