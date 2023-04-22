package org.lime.gp.player.level;

import java.util.HashMap;

import com.google.gson.JsonObject;

public class LevelData {
    public final int work;
    public final HashMap<Integer, LevelStep> levels = new HashMap<>();

    public LevelData(int work, JsonObject json) {
        this.work = work;
        json.entrySet().forEach(kv -> {
            int level = Integer.parseInt(kv.getKey());
            this.levels.put(level, new LevelStep(level, this, kv.getValue().getAsJsonObject()));
        });
    }
}
