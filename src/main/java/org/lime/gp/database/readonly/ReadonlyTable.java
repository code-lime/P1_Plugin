package org.lime.gp.database.readonly;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.lime.gp.lime;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;

public class ReadonlyTable<T> {
    private static final ConcurrentLinkedQueue<ReadonlyTable<?>> tables = new ConcurrentLinkedQueue<>();
    public static void sync() { tables.forEach(ReadonlyTable::updateReadonly); }

    private final String table;
    private final String key;
    private final Class<?> tKeyClass;
    private final Func1<Object, T> from;
    private final List<String> keys;
    private final Func0<Map<T, List<Object>>> data;
    private final Action1<String> debug;

    private final ConcurrentHashMap<Object, Integer> cooldowns = new ConcurrentHashMap<>();

    private ReadonlyTable(ReadonlyTable.Builder<T> builder) {
        this.table = builder.table;
        this.key = builder.key;
        this.tKeyClass = builder.tKeyClass;
        this.from = builder.from;
        this.keys = builder.keys;
        this.data = builder.data;
        this.debug = builder.debug;

        tables.add(this);
    }

    private static boolean compare(List<String> line1, List<String> line2, Action1<String> debug) {
        int length = line1.size();
        if (length != line2.size()) return false;
        for (int i = 0; i < length; i++) {
            if (Objects.equals(line1.get(i), line2.get(i))) continue;
            if (debug != null)
                debug.invoke("NotEquals[" + i + "]: " + line1.get(i) + " / " + line2.get(i));
            return false;
        }
        return true;
    }

    private static final long RETRY_UPDATE_MS = 30 * 1000;
    private final LockToast1<Long> lastUpdateMs = Toast.lock(0L);
    private void updateReadonly() {
        long nowMs = System.currentTimeMillis();
        if (lastUpdateMs.get0() + RETRY_UPDATE_MS > nowMs) return;
        lastUpdateMs.set0(nowMs);
        Methods.SQL.Async.rawSqlQuery("SELECT "+key+(keys.size() == 0 ? "" : (", " + String.join(", ", keys))) + " FROM "+table, reader -> {
            int size = MySql.columnsCount(reader);
            List<String> args = new ArrayList<>();
            for (int i = 2; i <= size; i++) args.add(MySql.toSqlObject(MySql.readObject(reader, i)));
            return Toast.of(from.invoke(MySql.readObject(reader, 1, tKeyClass)), args);
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
                    if (compare(_key.val1, _args.stream().map(MySql::toSqlObject).collect(Collectors.toList()), debug)) ignore.add(_key.val0);
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
            if (debug != null)
                debug.invoke("Update count: " + update.size());

            ConcurrentLinkedQueue<Toast2<String, Action0>> queue = new ConcurrentLinkedQueue<>();
            if (!update.isEmpty()) queue.add(Toast.of("INSERT INTO "+table+" ("+ String.join(",", keys) +") VALUES ("
                    + String.join("),(", update) +
                    ") ON DUPLICATE KEY UPDATE " +
                    keys.stream().map(v -> v + "=VALUES(" + v + ")").collect(Collectors.joining(",")),
                    () -> cooldowns.entrySet().removeIf(kv -> updateKeys.contains(kv.getKey()))));
            if (!remove.isEmpty()) queue.add(Toast.of("DELETE FROM "+table+" WHERE "+key+" IN ("+String.join(",", remove)+")", Execute.actionEmpty()));
            executeSqlQueue(queue, () -> lastUpdateMs.set0(0L));
        });
    }

    private static void executeSqlQueue(ConcurrentLinkedQueue<Toast2<String, Action0>> queue, Action0 onFinally) {
        Toast2<String, Action0> item = queue.poll();
        if (item == null) {
            onFinally.invoke();
            return;
        }
        Methods.SQL.Async.rawSql(item.val0, item.val1).withFinally(() -> executeSqlQueue(queue, onFinally));
    }

    public static class Builder<T> {
        private final String table;
        private final String key;
        private final Class<?> tKeyClass;
        private final Func1<Object, T> from;
        private final List<String> keys;
        private final Func0<Map<T, List<Object>>> data;
        private final Action1<String> debug;

        private Builder(String table, String key, Class<?> tKeyClass, Func1<Object, T> from, List<String> keys, Func0<Map<T, List<Object>>> data, Action1<String> debug) {
            this.table = table;
            this.key = key;
            this.tKeyClass = tKeyClass;
            this.from = from;
            this.keys = keys;
            this.data = data;
            this.debug = debug;
        }

        public static <T>ReadonlyTable.Builder<T> of(String table, String key) { return new ReadonlyTable.Builder<>(table, key, null, null, null, null, null); }

        @SuppressWarnings("unchecked")
        public <TKey>ReadonlyTable.Builder<T> withKey(Class<TKey> tKeyClass, Func1<TKey, T> from) {
            return new ReadonlyTable.Builder<T>(table, key, tKeyClass, v -> from.invoke((TKey)v), keys, data, debug);
        }
        public ReadonlyTable.Builder<T> withKey(Class<T> tKeyClass) { return withKey(tKeyClass, v -> v); }
        public ReadonlyTable.Builder<T> withKeys(List<String> keys) { return new ReadonlyTable.Builder<>(table, key, tKeyClass, from, keys, data, debug); }
        public ReadonlyTable.Builder<T> withKeys(String... keys) { return withKeys(Arrays.asList(keys)); }
        public ReadonlyTable.Builder<T> withData(Func0<Map<T, List<Object>>> data) { return new ReadonlyTable.Builder<>(table, key, tKeyClass, from, keys, data, debug); }
        public ReadonlyTable.Builder<T> withDebug(Action1<String> debug) { return new ReadonlyTable.Builder<>(table, key, tKeyClass, from, keys, data, debug); }

        public ReadonlyTable<T> build() { return new ReadonlyTable<>(this); }
    }
}