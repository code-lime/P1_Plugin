package org.lime.gp.database;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.lime.core;
import org.lime.gp.admin.BanList;
import org.lime.gp.craft.RecipesBook;
import org.lime.gp.database.rows.AAnyRow;
import org.lime.gp.database.rows.AnyRow;
import org.lime.gp.database.rows.BanListRow;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.rows.CompassTargetRow;
import org.lime.gp.database.rows.DiscordRow;
import org.lime.gp.database.rows.FriendRow;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.database.rows.HouseSubsRow;
import org.lime.gp.database.rows.PermissionRow;
import org.lime.gp.database.rows.PetsRow;
import org.lime.gp.database.rows.PreDonateItemsRow;
import org.lime.gp.database.rows.PreDonateRow;
import org.lime.gp.database.rows.PrisonRow;
import org.lime.gp.database.rows.RolesRow;
import org.lime.gp.database.rows.SmsPresetRow;
import org.lime.gp.database.rows.UserCraftsRow;
import org.lime.gp.database.rows.UserFlagsRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.rows.Variable;
import org.lime.gp.lime;
import org.lime.gp.player.module.PredonateWhitelist;
import org.lime.gp.player.perm.Perms;
import org.lime.system;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Tables {
    public static String valueOfInt(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }
    public static core.element create() {
        return core.element.create(Tables.class)
                .withInit(Tables::init);
    }
    public static void init() {
        lime.repeat(Tables::update, 1);
    }
    public static void update() {
        KeyedTable.updateAll();
    }

    public static MySql.debug getTable(String table, system.Action1<Tables.ITable<? extends BaseRow>> callback) {
        if (table.startsWith("!sql ")) return Methods.SQL.Async.rawSqlQuery(table.substring(5), AnyRow::new, rows -> callback.invoke(new Tables.StaticTable<>(rows)));
        callback.invoke(Tables.KeyedTable.tables.get(table));
        return new MySql.debug();
    }
    public static Tables.KeyedTable<? extends BaseRow> getLoadedTable(String table) {
        return Tables.KeyedTable.tables.getOrDefault(table, null);
    }
    public static List<system.Toast2<String, String>> getListRow(String prefix, BaseRow row) {
        return row.appendToReplace(new HashMap<>()).entrySet().stream().map(kv -> system.toast(prefix + kv.getKey(), kv.getValue())).collect(Collectors.toList());
    }

    public static final KeyedTable<AAnyRow> ABAN_TABLE = KeyedTable.of("aban", AAnyRow::new).keyed("id", v -> v.id + "").build();
    public static final KeyedTable<AAnyRow> AMUTE_TABLE = KeyedTable.of("amute", AAnyRow::new).keyed("id", v -> v.id + "").build();
    public static final KeyedTable<RolesRow> ROLES_TABLE = KeyedTable.of("roles", RolesRow::new).keyed("id", v -> v.id + "").build();
    //public static final KeyedTable<MaterialContainerRow> MATERIAL_CONTAINER_TABLE = KeyedTable.of("material_container", MaterialContainerRow::new).keyed("id", v -> v.ID + "").build();
    public static final KeyedTable<UserRow> USER_TABLE = KeyedTable.of("users", UserRow::new)
            .keyed("id", v -> v.id + "")
            .other("uuid", v -> v.uuid.toString())
            .event(KeyedTable.Event.Removed, RecipesBook::editRow)
            .event(KeyedTable.Event.Updated, RecipesBook::editRow)
            .build();
    public static final KeyedTable<HouseRow> HOUSE_TABLE = KeyedTable.of("house", HouseRow::new).keyed("id", v -> v.id + "").build();
    public static final KeyedTable<HouseSubsRow> HOUSE_SUBS_TABLE = KeyedTable.of("house_subs", HouseSubsRow::new).keyed("id", v -> v.id + "").build();
    public static final KeyedTable<FriendRow> FRIEND_TABLE = KeyedTable.of("friends", FriendRow::new).where("friends.friend_name IS NOT NULL").keyed("id", v -> v.id + "").build();

    public static final KeyedTable<PrisonRow> PRISON_TABLE = KeyedTable.of("prison", PrisonRow::new).where("prison.is_log = 0").keyed("id", v -> v.id + "").build();
    public static final KeyedTable<Variable> VARIABLE_TABLE = KeyedTable.of("variable", Variable::new).keyed("tmp", v -> "0").build();
    public static final KeyedTable<DiscordRow> DISCORD_TABLE = KeyedTable.of("discord", DiscordRow::new).keyed("discord_id", v -> v.discordID + "").build();
    public static final KeyedTable<CompassTargetRow> COMPASS_TARGET_TABLE = KeyedTable.of("compass_target", CompassTargetRow::new).keyed("id", v -> v.id + "").build();
    public static final KeyedTable<PetsRow> PETS_TABLE = KeyedTable.of("pets", PetsRow::new).keyed("id", v -> v.id + "").build();
    public static final KeyedTable<PermissionRow> PERMISSIONS_TABLE = KeyedTable.of("permissions", PermissionRow::new).keyed("uuid", v -> v.uuid.toString()).event(KeyedTable.Event.Removed, PermissionRow::removed).build();
    public static final KeyedTable<UserFlagsRow> USERFLAGS_TABLE = KeyedTable.of("user_flags", UserFlagsRow::new).where("user_flags.backpack_id > 0").keyed("id", v -> v.id + "").other("uuid", v -> v.uuid.toString()).build();
    public static final KeyedTable<UserCraftsRow> USERCRAFTS_TABLE = KeyedTable.of("user_crafts", UserCraftsRow::new)
            .keyed("id", v -> v.id + "")
            .event(KeyedTable.Event.Removed, row -> UserRow.getBy(row.uuid).ifPresent(RecipesBook::editRow))
            .event(KeyedTable.Event.Updated, row -> UserRow.getBy(row.uuid).ifPresent(RecipesBook::editRow))
            .event(KeyedTable.Event.Removed, Perms::onUserCraftUpdate)
            .event(KeyedTable.Event.Updated, Perms::onUserCraftUpdate)
            .build();
    public static final KeyedTable<BanListRow> BANLIST_TABLE = KeyedTable.of("ban_list", BanListRow::new)
            .keyed("id", v -> v.id + "")
            .other("user", v -> v.user)
            .event(KeyedTable.Event.Removed, BanList::onBanUpdate)
            .event(KeyedTable.Event.Updated, BanList::onBanUpdate)
            .build();
    public static final KeyedTable<PreDonateRow> PREDONATE_TABLE = KeyedTable.of("predonate", PreDonateRow::new)
            .keyed("id", v -> v.id + "")
            .event(KeyedTable.Event.Removed, PredonateWhitelist::onUpdate)
            .event(KeyedTable.Event.Updated, PredonateWhitelist::onUpdate)
            .build();
    public static final KeyedTable<PreDonateItemsRow> PREDONATE_ITEMS_TABLE = KeyedTable.of("predonate_items", PreDonateItemsRow::new).where("predonate_items.amount > 0").keyed("id", v -> v.id + "").build();
    public static final KeyedTable<SmsPresetRow> SMSPRESET_TABLE = KeyedTable.of("sms_preset", SmsPresetRow::new).keyed("id", v -> v.id + "").build();

    public static abstract class ITable<V extends BaseRow> {
        public abstract List<V> getRows();
        public abstract void forEach(system.Action1<V> callback);
        public Optional<V> getBy(system.Func1<V, Boolean> func) {
            for (V item : getRows()) {
                if (func.invoke(item))
                    return Optional.ofNullable(item);
            }
            return Optional.empty();
        }
        public boolean hasBy(system.Func1<V, Boolean> func) {
            for (V item : getRows()) {
                if (func.invoke(item))
                    return true;
            }
            return false;
        }
        public <T1>HashMap<T1, V> getMapBy(system.Func1<V, Boolean> compare, system.Func1<V, T1> convert) {
            HashMap<T1, V> map = new HashMap<>();
            getRows().forEach(v -> {
                if (!compare.invoke(v)) return;
                map.put(convert.invoke(v), v);
            });
            return map;
        }
        public <T1>HashMap<T1, V> getMap(system.Func1<V, T1> convert) {
            HashMap<T1, V> map = new HashMap<>();
            getRows().forEach(v -> map.put(convert.invoke(v), v));
            return map;
        }
        public List<V> getRowsBy(system.Func1<V, Boolean> compare) {
            List<V> list = new ArrayList<>();
            getRows().forEach(v -> {
                if (!compare.invoke(v)) return;
                list.add(v);
            });
            return list;
        }
        public Optional<V> getFirstRow() {
            List<V> list = getRows();
            return list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
        }
    }

    public static class StaticTable<V extends BaseRow> extends ITable<V> {
        private final ImmutableList<V> data;
        public StaticTable(List<V> data) {this.data = ImmutableList.copyOf(data); }
        @Override public List<V> getRows() { return data; }
        @Override public void forEach(system.Action1<V> callback) { data.forEach(callback); }
    }
    public static class KeyedTable<V extends BaseRow> extends ITable<V> {
        public static final ConcurrentHashMap<String, KeyedTable<?>> tables = new ConcurrentHashMap<>();

        private Calendar last_update = system.getZeroTime();

        private boolean isInit = false;
        
        @SuppressWarnings("unused")
        private final String index;
        private final String table;
        private final String where_select;
        private final String where;
        private final String key;
        private final system.Func2<ResultSet, V, String> fKey;
        private final system.Func1<ResultSet, V> fValue;
        private final Map<String, system.Func1<V, String>> other;
        private final Map<Event, List<system.Action2<V, Event>>> events;

        private final ConcurrentHashMap<String, V> data = new ConcurrentHashMap<>();

        private class OtherData {
            @SuppressWarnings("unused")
            public final String other_index;
            public final ConcurrentHashMap<String, V> data = new ConcurrentHashMap<>();
            public final ConcurrentHashMap<String, String> keyed_remove = new ConcurrentHashMap<>();

            public OtherData(String other_index) {
                this.other_index = other_index;
            }

            public void add(String key, String other_key, V value) {
                keyed_remove.compute(key, (k,v) -> {
                    if (v != null && !v.equals(other_key)) data.remove(v);
                    data.put(other_key, value);
                    return other_key;
                });
            }
            public void remove(String key) {
                String other_key = keyed_remove.remove(key);
                if (other_key == null) return;
                data.remove(other_key);
            }
        }

        private final ConcurrentHashMap<String, OtherData> otherData = new ConcurrentHashMap<>();

        public boolean isInit() { return this.isInit; }

        private KeyedTable(String index, String table, String where, String key, system.Func2<ResultSet, V, String> fKey, system.Func1<ResultSet, V> fValue, Map<String, system.Func1<V, String>> other, ImmutableList<system.Toast2<Event, system.Action2<V, Event>>> events) {
            this.index = index;
            this.table = table;
            this.where_select = "last_update > @last" + (where == null ? "" : (" AND (" + where + ")"));
            this.where = where;
            this.key = key;
            this.fKey = fKey;
            this.fValue = fValue;
            this.other = other;
            Map<Event, List<system.Action2<V, Event>>> _events = new HashMap<>();
            events.forEach(kv -> _events.computeIfAbsent(kv.val0, k -> new ArrayList<>()).add(kv.val1));
            this.events = ImmutableMap.copyOf(_events);
            this.events.forEach((event, calls) -> lime.logOP("Reg event '"+table+"'.'"+event+"' with calls: "+calls.size()));
            tables.put(index, this);
        }
        @Override public List<V> getRows() { return ImmutableList.copyOf(data.values()); }
        @Override public void forEach(system.Action1<V> callback) { data.values().forEach(callback); }
        public Map<String, V> getKeyedRows() { return data; }
        public Optional<V> get(String key) { return Optional.ofNullable(data.get(key)); }
        public boolean has(String key) { return data.containsKey(key); }
        public Optional<V> getOther(String other, String key) {
            return Optional.ofNullable(otherData.get(other)).map(v -> v.data.get(key));
        }
        public boolean hasOther(String other, String key) {
            OtherData _data = otherData.getOrDefault(other, null);
            return _data != null && _data.data.containsKey(key);
        }
        public static void updateAll() {
            tables.forEach((key, table) -> table.update());
        }
        protected void update() {
            Calendar next_update = system.getMoscowNow();
            next_update.add(Calendar.SECOND, -5);
            Methods.SQL.Async.rawSqlQuery(
                    "SELECT * FROM " + table + " WHERE " + where_select,
                    MySql.args().add("last", last_update).build(),
                    set -> { V value = fValue.invoke(set); return system.toast(fKey.invoke(set, value), value); },
                    _rows -> {
                        _rows.forEach(kv -> {
                            kv.val1.init();
                            data.put(kv.val0, kv.val1);
                            other.forEach((k, v) -> otherData.compute(k, (_k, _v) -> {
                                String other_key = v.invoke(kv.val1);
                                if (_v == null) _v = new OtherData(k);
                                _v.add(kv.val0, other_key, kv.val1);
                                return _v;
                            }));
                            onEvent(Event.Updated, kv.val1);
                        });
                        isInit = true;
                    }
            );
            Methods.SQL.Async.rawSqlQuery(
                    "SELECT "+table+"."+key+" FROM " + table + (where == null ? "" : " WHERE " + where),
                    String.class,
                    _rows -> {
                        HashSet<String> delete = new HashSet<>(this.data.keySet());
                        _rows.forEach(delete::remove);
                        this.data.entrySet().removeIf(kv -> {
                            if (!delete.contains(kv.getKey())) return false;
                            other.forEach((k, v) -> otherData.compute(k, (_k, _v) -> {
                                if (_v == null) _v = new OtherData(k);
                                _v.remove(kv.getKey());
                                return _v;
                            }));
                            onEvent(Event.Removed, kv.getValue());
                            return true;
                        });
                    }
            );

            last_update = next_update;
        }

        private void onEvent(Event event, V value) {
            List<system.Action2<V, Event>> calls = events.getOrDefault(event, null);
            if (calls == null) return;
            for (system.Action2<V, Event> call : calls) call.invoke(value, event);
        }

        public enum Event {
            Updated(true, false),
            Removed(false, true);

            public final boolean updated;
            public final boolean removed;

            Event(boolean updated, boolean removed) {
                this.updated = updated;
                this.removed = removed;
            }
        }

        public static class Builder<V extends BaseRow> {
            private final String _index;
            private final String _table;
            private final String _where;
            private final String _key;
            private final system.Func2<ResultSet, V, String> _fKey;
            private final system.Func1<ResultSet, V> _fValue;
            private final ImmutableMap<String, system.Func1<V, String>> _other;
            private final ImmutableList<system.Toast2<Event, system.Action2<V, Event>>> _events;

            private Builder(String index, String table, String where, String key, system.Func2<ResultSet, V, String> fKey, system.Func1<ResultSet, V> fValue, ImmutableMap<String, system.Func1<V, String>> other, ImmutableList<system.Toast2<Event, system.Action2<V, Event>>> events) {
                this._index = index;
                this._table = table;
                this._where = where;
                this._key = key;
                this._fKey = fKey;
                this._fValue = fValue;
                this._other = other;
                this._events = events;
            }

            public Builder<V> index(String index) { return new Builder<V>(index, _table, _where, _key, _fKey, _fValue, _other, _events); }
            public Builder<V> where(String where) { return new Builder<V>(_index, _table, where, _key, _fKey, _fValue, _other, _events); }
            public Builder<V> key(String key, system.Func1<ResultSet, String> func) { return new Builder<V>(_index, _table, _where, key, (v1,v2) -> func.invoke(v1), _fValue, _other, _events); }
            public Builder<V> keyed(String key, system.Func1<V, String> func) { return new Builder<V>(_index, _table, _where, key, (v1,v2) -> func.invoke(v2), _fValue, _other, _events); }
            public Builder<V> other(String key, system.Func1<V, String> func) { return new Builder<V>(_index, _table, _where, _key, _fKey, _fValue, ImmutableMap.<String, system.Func1<V, String>>builder().putAll(_other).put(key, func).build(), _events); }

            /*public Builder<V> event(Event key, system.Action2<V, Event> func) { return event(key, (a,b) -> { func.invoke(a,b); return true; }); }
            public Builder<V> event(Event key, system.Action1<V> func) { return event(key, (a,b) -> { func.invoke(a); return true; }); }
*/
            public Builder<V> event(Event key, system.Action2<V, Event> func) { return new Builder<>(_index, _table, _where, _key, _fKey, _fValue, _other, ImmutableList.<system.Toast2<Event, system.Action2<V, Event>>>builder().addAll(_events).add(system.toast(key, func)).build()); }
            public Builder<V> event(Event key, system.Action1<V> func) { return event(key, (a,b) -> func.invoke(a)); }

            public Builder<V> value(system.Func1<ResultSet, V> func) { return new Builder<V>(_index, _table, _where, _key, _fKey, func, _other, _events); }

            public KeyedTable<V> build() { return new KeyedTable<>(_index, _table, _where, _key, _fKey, _fValue, _other, _events); }
        }

        public static <V extends BaseRow>Builder<V> of(String table, system.Func1<ResultSet, V> value) { return new Builder<>(table, table, null, null, null, value, ImmutableMap.of(), ImmutableList.of()); }
    }
}













