package org.lime.gp.player.perm;

import com.google.gson.JsonObject;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.gp.database.rows.UserRow;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class Levels {
    public static core.element create() {
        return core.element.create(Levels.class)
                .<JsonObject>addConfig("levels", v -> v.withInvoke(Levels::config).withDefault(new JsonObject()));
    }
    public static final HashMap<Integer, LevelData> levels = new HashMap<>();

    public static class LevelData {
        public final int id;

        public final String name;
        public final String icon;

        public final Perms.CanData canData;

        public LevelData(int id, JsonObject json) {
            this.id = id;
            name = json.get("name").getAsString();
            icon = json.get("icon").getAsString();

            canData = new Perms.CanData(json);
        }
    }
    public static Optional<LevelData> getLevelData(UUID uuid) {
        return UserRow.getBy(uuid).map(v -> v.work).map(levels::get);
    }

    public static void config(JsonObject json) {
        HashMap<Integer, LevelData> _levels = new HashMap<>();
        lime.combineParent(json).entrySet().forEach(kv -> _levels.put(Integer.parseInt(kv.getKey()), new LevelData(Integer.parseInt(kv.getKey()), kv.getValue().getAsJsonObject())));
        levels.clear();
        levels.putAll(_levels);
    }
}
