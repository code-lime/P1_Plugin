package org.lime.gp.database.tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.lime.system;
import org.lime.gp.database.rows.BaseRow;

public abstract class ITable<V extends BaseRow> {
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