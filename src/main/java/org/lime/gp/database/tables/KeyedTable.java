package org.lime.gp.database.tables;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.database.Methods;
import org.lime.gp.database.MySql;
import org.lime.gp.database.rows.BaseRow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class KeyedTable<V extends BaseRow> extends ITable<V> {
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
        private final Map<KeyedTable.Event, List<system.Action2<V, KeyedTable.Event>>> events;

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

        private KeyedTable(String index, String table, String where, String key, system.Func2<ResultSet, V, String> fKey, system.Func1<ResultSet, V> fValue, Map<String, system.Func1<V, String>> other, ImmutableList<system.Toast2<KeyedTable.Event, system.Action2<V, KeyedTable.Event>>> events) {
            this.index = index;
            this.table = table;
            this.where_select = "last_update > @last" + (where == null ? "" : (" AND (" + where + ")"));
            this.where = where;
            this.key = key;
            this.fKey = fKey;
            this.fValue = fValue;
            this.other = other;
            Map<KeyedTable.Event, List<system.Action2<V, KeyedTable.Event>>> _events = new HashMap<>();
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

        private void onEvent(KeyedTable.Event event, V value) {
            List<system.Action2<V, KeyedTable.Event>> calls = events.getOrDefault(event, null);
            if (calls == null) return;
            for (system.Action2<V, KeyedTable.Event> call : calls) call.invoke(value, event);
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
            private final ImmutableList<system.Toast2<KeyedTable.Event, system.Action2<V, KeyedTable.Event>>> _events;

            private Builder(String index, String table, String where, String key, system.Func2<ResultSet, V, String> fKey, system.Func1<ResultSet, V> fValue, ImmutableMap<String, system.Func1<V, String>> other, ImmutableList<system.Toast2<KeyedTable.Event, system.Action2<V, KeyedTable.Event>>> events) {
                this._index = index;
                this._table = table;
                this._where = where;
                this._key = key;
                this._fKey = fKey;
                this._fValue = fValue;
                this._other = other;
                this._events = events;
            }

            public KeyedTable.Builder<V> index(String index) { return new KeyedTable.Builder<V>(index, _table, _where, _key, _fKey, _fValue, _other, _events); }
            public KeyedTable.Builder<V> where(String where) { return new KeyedTable.Builder<V>(_index, _table, where, _key, _fKey, _fValue, _other, _events); }
            public KeyedTable.Builder<V> key(String key, system.Func1<ResultSet, String> func) { return new KeyedTable.Builder<V>(_index, _table, _where, key, (v1,v2) -> func.invoke(v1), _fValue, _other, _events); }
            public KeyedTable.Builder<V> keyed(String key, system.Func1<V, String> func) { return new KeyedTable.Builder<V>(_index, _table, _where, key, (v1,v2) -> func.invoke(v2), _fValue, _other, _events); }
            public KeyedTable.Builder<V> other(String key, system.Func1<V, String> func) { return new KeyedTable.Builder<V>(_index, _table, _where, _key, _fKey, _fValue, ImmutableMap.<String, system.Func1<V, String>>builder().putAll(_other).put(key, func).build(), _events); }

            /*public Builder<V> event(Event key, system.Action2<V, Event> func) { return event(key, (a,b) -> { func.invoke(a,b); return true; }); }
            public Builder<V> event(Event key, system.Action1<V> func) { return event(key, (a,b) -> { func.invoke(a); return true; }); }
*/
            public KeyedTable.Builder<V> event(KeyedTable.Event key, system.Action2<V, KeyedTable.Event> func) { return new KeyedTable.Builder<>(_index, _table, _where, _key, _fKey, _fValue, _other, ImmutableList.<system.Toast2<KeyedTable.Event, system.Action2<V, KeyedTable.Event>>>builder().addAll(_events).add(system.toast(key, func)).build()); }
            public KeyedTable.Builder<V> event(KeyedTable.Event key, system.Action1<V> func) { return event(key, (a,b) -> func.invoke(a)); }

            public KeyedTable.Builder<V> value(system.Func1<ResultSet, V> func) { return new KeyedTable.Builder<V>(_index, _table, _where, _key, _fKey, func, _other, _events); }

            public KeyedTable<V> build() { return new KeyedTable<>(_index, _table, _where, _key, _fKey, _fValue, _other, _events); }
        }

        public static <V extends BaseRow>KeyedTable.Builder<V> of(String table, system.Func1<ResultSet, V> value) { return new KeyedTable.Builder<>(table, table, null, null, null, value, ImmutableMap.of(), ImmutableList.of()); }
    }