package org.lime.gp.player.perm;

import com.google.gson.JsonObject;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.gp.database.rows.RolesRow;
import org.lime.gp.database.rows.UserRow;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class Grants {
    public static core.element create() {
        return core.element.create(Grants.class)
                .<JsonObject>addConfig("grants", v -> v.withInvoke(Grants::config).withDefault(new JsonObject()));
    }

    public static final HashMap<Integer, GrantData> grants = new HashMap<>();

    public static class GrantData {
        public final int id;
        public final int payday;
        public final boolean magnifier;
        //public final HashMap<Integer, List<Integer>> grantRoles = new HashMap<>();
        public final HashMap<Integer, Perms.ICanData> worksCans = new HashMap<>();

        public final Perms.CanData canData;

        public GrantData(int id, JsonObject json) {
            this.id = id;
            this.payday = json.has("payday") ? json.get("payday").getAsInt() : 0;
            this.magnifier = json.has("magnifier") && json.get("magnifier").getAsBoolean();
            this.canData = new Perms.CanData(json);
            if (json.has("works")) json.get("works").getAsJsonObject().entrySet().forEach(kv -> worksCans.put(Integer.parseInt(kv.getKey()), new Perms.CanData(kv.getValue().getAsJsonObject())));
        }
    }
    public static Optional<GrantData> getGrantData(UUID uuid) { return UserRow.getBy(uuid).flatMap(Grants::getGrantData); }
    public static Optional<GrantData> getGrantData(int id) { return UserRow.getBy(id).flatMap(Grants::getGrantData); }
    public static Optional<GrantData> getGrantData(UserRow user) { return Optional.ofNullable(user).flatMap(v -> RolesRow.getBy(user.role)).map(v -> grants.get(v.permissions)); }

    public static void config(JsonObject json) {
        HashMap<Integer, GrantData> _grants = new HashMap<>();
        lime.combineParent(json).entrySet().forEach(kv -> {
            int grant = Integer.parseInt(kv.getKey());
            _grants.put(grant, new GrantData(grant, kv.getValue().getAsJsonObject()));
        });
        grants.clear();
        grants.putAll(_grants);
    }
}
