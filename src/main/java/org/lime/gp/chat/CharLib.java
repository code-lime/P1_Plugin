package org.lime.gp.chat;

import com.google.gson.JsonObject;
import org.lime.gp.lime;
import org.bukkit.entity.Player;
import org.lime.system;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class CharLib {
    public final Map<Character, Integer> sizeMap;
    public final Map<Integer, String> spaceSize;

    private CharLib(Map<Character, Integer> sizeMap, Map<Integer, String> spaceSize) {
        this.sizeMap = sizeMap;
        this.spaceSize = spaceSize;
    }

    private static final CharLib DEFAULT;
    private static final CharLib MINI;

    static {
        HashMap<Character, Integer> sizeMap = new HashMap<>();
        HashMap<Integer, String> spaceSize = new HashMap<>();
        
        HashMap<Character, Integer> sizeMapMini = new HashMap<>();
        HashMap<Integer, String> spaceSizeMini = new HashMap<>();

        try (InputStream stream = lime._plugin.getResource("symbols.json")) {
            JsonObject json = system.json.parse(new String(stream.readAllBytes())).getAsJsonObject();
            json.getAsJsonObject("size").entrySet().forEach(kv -> sizeMap.put(kv.getKey().charAt(0), kv.getValue().getAsInt()));
            json.getAsJsonObject("space").entrySet().forEach(kv -> spaceSize.put(Integer.parseInt(kv.getKey()), kv.getValue().getAsString()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        sizeMapMini.putAll(sizeMap);
        spaceSizeMini.putAll(spaceSize);

        try (InputStream stream = lime._plugin.getResource("symbols.mini.json")) {
            JsonObject json = system.json.parse(new String(stream.readAllBytes())).getAsJsonObject();
            json.getAsJsonObject("size").entrySet().forEach(kv -> sizeMapMini.put(kv.getKey().charAt(0), kv.getValue().getAsInt()));
            json.getAsJsonObject("space").entrySet().forEach(kv -> spaceSizeMini.put(Integer.parseInt(kv.getKey()), kv.getValue().getAsString()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        DEFAULT = new CharLib(sizeMap, spaceSize);
        MINI = new CharLib(sizeMapMini, spaceSizeMini);
    }

    public static CharLib getCharLib(Player player) {
        return player != null && player.getScoreboardTags().contains("symbols.mini") ? MINI : DEFAULT;
    }
}
