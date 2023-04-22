package org.lime.gp.database;

import com.google.gson.JsonArray;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.lime.core;
import org.lime.gp.admin.Administrator;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.Skins;
import org.lime.gp.player.perm.Works;
import org.lime.gp.player.selector.ISelector;
import org.lime.gp.player.selector.SelectorType;
import org.lime.gp.player.voice.Radio;
import org.lime.system;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.player.selector.UserSelector;
import org.lime.gp.player.module.TabManager;
import org.lime.gp.player.module.Death;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ReadonlySync {
    public static core.element create() {
        return core.element.create(ReadonlySync.class)
                .withInit(ReadonlySync::init);
    }
    public static void init() {
        lime.repeat(ReadonlySync::update, 1);
    }
    public static void update() {
        Readonly.sync();
    }

    public static class Readonly<T> {
        private static final ConcurrentLinkedQueue<Readonly<?>> tables = new ConcurrentLinkedQueue<>();
        public static void sync() { tables.forEach(Readonly::updateReadonly); }

        private final String table;
        private final String key;
        private final Class<?> tKeyClass;
        private final system.Func1<Object, T> from;
        private final List<String> keys;
        private final system.Func0<Map<T, List<Object>>> data;

        private final ConcurrentHashMap<Object, Integer> cooldowns = new ConcurrentHashMap<>();

        private Readonly(Builder<T> builder) {
            this.table = builder.table;
            this.key = builder.key;
            this.tKeyClass = builder.tKeyClass;
            this.from = builder.from;
            this.keys = builder.keys;
            this.data = builder.data;

            tables.add(this);
        }

        private static boolean compare(List<String> line1, List<String> line2) {
            int length = line1.size();
            if (length != line2.size()) return false;
            for (int i = 0; i < length; i++) {
                if (Objects.equals(line1.get(i), line2.get(i))) continue;
                return false;
            }
            return true;
        }

        private void updateReadonly() {
            Methods.SQL.Async.rawSqlQuery("SELECT "+key+(keys.size() == 0 ? "" : (", " + String.join(", ", keys))) + " FROM "+table, reader -> {
                int size = MySql.columnsCount(reader);
                List<String> args = new ArrayList<>();
                for (int i = 2; i <= size; i++) args.add(MySql.toSqlObject(MySql.readObject(reader, i)));
                return system.toast(from.invoke(MySql.readObject(reader, 1, tKeyClass)), args);
            }, (callback) -> {
                Map<T, List<Object>> data = this.data.invoke();
                List<String> remove = new ArrayList<>();
                List<Object> ignore = new ArrayList<>();
                List<String> update = new ArrayList<>();
                List<Object> updateKeys = new ArrayList<>();
                HashMap<Object, Integer> map = new HashMap<>();
                cooldowns.entrySet().removeIf(kv -> {
                    int value = kv.getValue() - 1;
                    if (value <= 0) return true;
                    map.put(kv.getKey(), value);
                    return false;
                });
                cooldowns.putAll(map);
                callback.forEach(_key -> {
                    if (data.containsKey(_key.val0)) {
                        List<Object> _args = data.getOrDefault(_key.val0, null);
                        if (_args == null) return;
                        if (compare(_key.val1, _args.stream().map(MySql::toSqlObject).collect(Collectors.toList()))) ignore.add(_key.val0);
                        return;
                    }
                    remove.add(MySql.toSqlObject(_key.val0));
                });
                data.forEach((_key,args) -> {
                    if (ignore.contains(_key)) return;
                    if (cooldowns.containsKey(_key)) return;
                    cooldowns.put(_key, 4);
                    updateKeys.add(_key);
                    update.add(args.stream().map(MySql::toSqlObject).collect(Collectors.joining(",")));
                });
                if (update.size() != 0)
                    Methods.SQL.Async.rawSql(
                            "INSERT INTO "+table+" ("+ String.join(",", keys) +") VALUES ("
                                    + String.join("),(", update) +
                                    ") ON DUPLICATE KEY UPDATE " +
                                    keys.stream().map(v -> v + "=VALUES(" + v + ")").collect(Collectors.joining(",")),
                            () -> cooldowns.entrySet().removeIf(kv -> updateKeys.contains(kv.getKey())));
                if (remove.size() != 0)
                    Methods.SQL.Async.rawSql(
                            "DELETE FROM "+table+" WHERE "+key+" IN ("+String.join(",", remove)+")",
                            () -> { });
            });
        }
        public static class Builder<T> {
            private final String table;
            private final String key;
            private final Class<?> tKeyClass;
            private final system.Func1<Object, T> from;
            private final List<String> keys;
            private final system.Func0<Map<T, List<Object>>> data;

            private Builder(String table, String key, Class<?> tKeyClass, system.Func1<Object, T> from, List<String> keys, system.Func0<Map<T, List<Object>>> data) {
                this.table = table;
                this.key = key;
                this.tKeyClass = tKeyClass;
                this.from = from;
                this.keys = keys;
                this.data = data;
            }

            public static <T>Builder<T> of(String table, String key) { return new Builder<>(table, key, null, null, null, null); }

            @SuppressWarnings("unchecked")
            public <TKey>Builder<T> withKey(Class<TKey> tKeyClass, system.Func1<TKey, T> from) {
                return new Builder<T>(table, key, tKeyClass, v -> from.invoke((TKey)v), keys, data);
            }
            public Builder<T> withKey(Class<T> tKeyClass) { return withKey(tKeyClass, v -> v); }
            public Builder<T> withKeys(List<String> keys) { return new Builder<>(table, key, tKeyClass, from, keys, data); }
            public Builder<T> withKeys(String... keys) { return withKeys(Arrays.asList(keys)); }
            public Builder<T> withData(system.Func0<Map<T, List<Object>>> data) { return new Builder<>(table, key, tKeyClass, from, keys, data); }

            public Readonly<T> build() { return new Readonly<>(this); }
        }
    }

    private static final Readonly<String> ONLINE_READONLY = Readonly.Builder.<String>of("online", "uuid")
            .withKey(String.class)
            .withKeys("uuid","x","y","z","world","timed_id","data_icon","data_name","is_op","zone_selector","die","gpose","hide","skin_url")
            .withData(() -> EntityPosition.onlinePlayers.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().toString(), kv -> {
                UUID uuid = kv.getKey();
                Player player = kv.getValue();
                Location location = player.getLocation();
                int world = Bukkit.getWorlds().indexOf(location.getWorld());
                Optional<Works.WorkData> data = Works.getWorkData(uuid);
                String selector_name = UserSelector.getSelector(player).map(ISelector::getType).map(SelectorType::getName).orElse(null);

                return Arrays.asList(
                        uuid,
                        system.round(location.getX(), 3),
                        system.round(location.getY(), 3),
                        system.round(location.getZ(), 3),
                        world,
                        TabManager.getPayerIDorNull(uuid),
                        data.map(v -> v.icon).orElse(null),
                        data.map(v -> v.name).orElse(null),
                        player.isOp(),
                        selector_name,
                        Death.isDamageLay(uuid),
                        lime.isLay(player) ? "LAY" : lime.isSit(player) ? "SIT" : "NONE",
                        HideNickSetting.isHide(player) ? 1 : 0,
                        Skins.getSkinURL(player)
                );
            })))
            .build();

    private static final Readonly<String> ABAN_TARGET_READONLY = Readonly.Builder.<String>of("aban_target", "CONCAT(IFNULL(data,''),'^',room_id,'^',type)")
            .withKey(String.class)
            .withKeys("data","room_id","type")
            .withData(() -> {
                HashMap<String, List<Object>> list = new HashMap<>();

                List<system.Toast3<Double, Location, String>> _rooms = new ArrayList<>(Administrator.rooms);
                int size = _rooms.size();
                for (int i = 0; i < size; i++) {
                    system.Toast3<Double, Location, String> kv = _rooms.get(i);
                    list.put((kv.val2 == null ? "" : kv.val2) + "^" + i + "^ROOM", Arrays.<Object>asList(kv.val2, i, "ROOM"));
                }
                Administrator.target_list.forEach((k, v) -> list.put(k + "^" + v + "^UUID", Arrays.<Object>asList(k.toString(), v, "UUID")));

                return list;
            })
            .build();

    private static final Readonly<String> WORK_READONLY = Readonly.Builder.<String>of("work_readonly", "CONCAT(id,'^',type)")
            .withKey(String.class)
            .withKeys("id","type","icon","name")
            .withData(() ->
                    Works.works.values().stream().map(v -> system.toast(v.id + "^WORK", Arrays.<Object>asList(v.id, "WORK", v.icon, v.name)))
                            .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1))
            )
            .build();

    @SuppressWarnings("deprecation")
    private static final Readonly<String> ENTITY_READONLY = Readonly.Builder.<String>of("entity", "uuid")
            .withKey(String.class)
            .withKeys("uuid","x","y","z","name","owner_uuid","type")
            .withData(() -> EntityPosition.getEntitiyRows().entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().toString(), kv -> {
                Tameable tameable = kv.getValue();
                Location location = tameable.getLocation();
                return Arrays.asList(
                        kv.getKey(),
                        system.round(location.getX(), 3),
                        system.round(location.getY(), 3),
                        system.round(location.getZ(), 3),
                        tameable.getCustomName(),
                        tameable.getOwnerUniqueId(),
                        tameable.getType().name()
                );
            })))
            .build();

    private static final Readonly<String> ENTITY_SUBS_READONLY = Readonly.Builder.<String>of("entity_subs", "CONCAT(uuid,'^',sub_uuid)")
            .withKey(String.class)
            .withKeys("uuid","sub_uuid")
            .withData(() -> EntityPosition.getEntitiyRows().values().stream().flatMap(j -> {
                JsonArray data = JManager.get(JsonArray.class, j.getPersistentDataContainer(), "sub_owners", null);
                if (data == null) return Stream.empty();
                List<system.Toast2<String, List<Object>>> list = new ArrayList<>();
                String uuid = j.getUniqueId().toString();
                data.forEach(v -> {
                    String other = v.getAsString();
                    list.add(system.toast(uuid + "^" + other, Arrays.<Object>asList(uuid, other)));
                });
                return list.stream();
            }).collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1)))
            .build();

    private static final Readonly<String> RADIO_READONLY = Readonly.Builder.<String>of("radio_readonly", "CONCAT(uuid,'^',level)")
            .withKey(String.class)
            .withKeys("uuid", "level")
            .withData(() -> Radio.elements()
                    .map(v -> v instanceof Radio.PlayerRadioElement p ? p : null)
                    .filter(Objects::nonNull)
                    .flatMap(radio -> {
                        String uuid = radio.player().getUniqueId().toString();
                        return radio.levels().stream().map(level -> system.toast(uuid + "^" + level, Arrays.<Object>asList(uuid, level + "")));
                    }).collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1))
            )
            .build();

}
