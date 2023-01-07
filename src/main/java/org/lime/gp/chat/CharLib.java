package org.lime.gp.chat;

import com.google.gson.JsonObject;
import org.lime.gp.lime;
import org.lime.system;

import java.io.InputStream;
import java.util.HashMap;

public final class CharLib {
    public static final HashMap<Character, Integer> sizeMap = new HashMap<>();
    public static final HashMap<Integer, String> spaceSize = new HashMap<>();
    static {
        try (InputStream stream = lime._plugin.getResource("symbols.json")) {
            JsonObject json = system.json.parse(new String(stream.readAllBytes())).getAsJsonObject();
            json.getAsJsonObject("size").entrySet().forEach(kv -> sizeMap.put(kv.getKey().charAt(0), kv.getValue().getAsInt()));
            json.getAsJsonObject("space").entrySet().forEach(kv -> spaceSize.put(Integer.parseInt(kv.getKey()), kv.getValue().getAsString()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
