package org.lime.gp.player.level;

import java.util.HashMap;

import com.google.gson.JsonObject;

public class LevelData {
    public final int work;
    public final HashMap<Integer, LevelStep> levels = new HashMap<>();
    public final HashMap<Integer, JsonObject> cache = new HashMap<>();

    public LevelData(int work, JsonObject json) {
        this.work = work;
        json.entrySet().forEach(kv -> {
            int level = Integer.parseInt(kv.getKey());
            cache.put(level, kv.getValue().getAsJsonObject());
            this.levels.put(level, new LevelStep(level, this, kv.getValue().getAsJsonObject()));
        });
    }
}
