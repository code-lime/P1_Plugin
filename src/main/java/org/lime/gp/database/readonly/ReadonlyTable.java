package org.lime.gp.database.readonly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.lime.system;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;

public class ReadonlyTable<T> {
    private static final ConcurrentLinkedQueue<ReadonlyTable<?>> tables = new ConcurrentLinkedQueue<>();
    public static void sync() { tables.forEach(ReadonlyTable::updateReadonly); }

    private final String table;
    private final String key;
    private final Class<?> tKeyClass;
    private final system.Func1<Object, T> from;
    private final List<String> keys;
    private final system.Func0<Map<T, List<Object>>> data;

    private final ConcurrentHashMap<Object, Integer> cooldowns = new ConcurrentHashMap<>();

    private ReadonlyTable(ReadonlyTable.Builder<T> builder) {
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

        public static <T>ReadonlyTable.Builder<T> of(String table, String key) { return new ReadonlyTable.Builder<>(table, key, null, null, null, null); }

        @SuppressWarnings("unchecked")
        public <TKey>ReadonlyTable.Builder<T> withKey(Class<TKey> tKeyClass, system.Func1<TKey, T> from) {
            return new ReadonlyTable.Builder<T>(table, key, tKeyClass, v -> from.invoke((TKey)v), keys, data);
        }
        public ReadonlyTable.Builder<T> withKey(Class<T> tKeyClass) { return withKey(tKeyClass, v -> v); }
        public ReadonlyTable.Builder<T> withKeys(List<String> keys) { return new ReadonlyTable.Builder<>(table, key, tKeyClass, from, keys, data); }
        public ReadonlyTable.Builder<T> withKeys(String... keys) { return withKeys(Arrays.asList(keys)); }
        public ReadonlyTable.Builder<T> withData(system.Func0<Map<T, List<Object>>> data) { return new ReadonlyTable.Builder<>(table, key, tKeyClass, from, keys, data); }

        public ReadonlyTable<T> build() { return new ReadonlyTable<>(this); }
    }
}