package org.lime.gp.player.perm;

import com.google.gson.JsonObject;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.gp.database.rows.UserRow;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class Works {
    public static core.element create() {
        return core.element.create(Works.class)
                .<JsonObject>addConfig("works", v -> v.withInvoke(Works::config).withDefault(new JsonObject()));
    }
    public static final HashMap<Integer, WorkData> works = new HashMap<>();

    public static class WorkData {
        public final int id;

        public final String name;
        public final String icon;

        public final Perms.CanData canData;

        public WorkData(int id, JsonObject json) {
            this.id = id;
            name = json.get("name").getAsString();
            icon = json.get("icon").getAsString();

            canData = new Perms.CanData(json);
        }
    }
    public static Optional<WorkData> getWorkData(UUID uuid) {
        return UserRow.getBy(uuid).map(v -> v.work).map(works::get);
    }

    public static void config(JsonObject json) {
        HashMap<Integer, WorkData> _works = new HashMap<>();
        lime.combineParent(json).entrySet().forEach(kv -> _works.put(Integer.parseInt(kv.getKey()), new WorkData(Integer.parseInt(kv.getKey()), kv.getValue().getAsJsonObject())));
        works.clear();
        works.putAll(_works);
    }
}
