package org.lime.gp.player.perm;

import com.google.gson.JsonObject;
import org.lime.gp.database.rows.RolesRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class Grants {
    public static CoreElement create() {
        return CoreElement.create(Grants.class)
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
    public static Stream<GrantData> getGrantData(UUID uuid) {
        return UserRow.getBy(uuid).stream().flatMap(Grants::getGrantData);
    }
    public static Stream<GrantData> getGrantData(int id) {
        return UserRow.getBy(id).stream().flatMap(Grants::getGrantData);
    }
    public static Stream<GrantData> getGrantData(UserRow user) {
        return Optional.ofNullable(user)
                .flatMap(v -> RolesRow.getBy(user.role))
                .stream()
                .flatMap(v -> v.permissions.stream().map(grants::get))
                .filter(Objects::nonNull);
    }

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
