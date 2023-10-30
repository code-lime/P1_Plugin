package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.lime.gp.chat.ChatHelper;
import org.lime.system.toast.*;

public abstract class BaseRow {
    protected BaseRow(ResultSet set) { }
    public Map<String, String> appendToReplace(Map<String, String> map) { return map; }
    public String applyToString(String line) { return applyToString(line, '{', '}'); }
    public String applyToString(String line, char start, char end) { return applyToString(line, start, end, new HashMap<>()); }
    @SuppressWarnings("unchecked")
    public String applyToString(String line, Toast2<String, String>... map) { return applyToString(line, '{', '}', map); }
    @SuppressWarnings("unchecked")
    public String applyToString(String line, char start, char end, Toast2<String, String>... map) {
        HashMap<String, String> _map = new HashMap<>();
        for (Toast2<String, String> kv : map) _map.put(kv.val0, kv.val1);
        return applyToString(line, start, end, _map);
    }
    public String applyToString(String line, HashMap<String, String> map) { return applyToString(line, '{', '}', map); }
    public String applyToString(String line, String prefix, HashMap<String, String> map) { return applyToString(line, '{', '}', prefix, map); }
    public String applyToString(String line, char start, char end, HashMap<String, String> map) {
        Map<String, String> _map = appendToReplace(new HashMap<>());
        _map.putAll(map);
        _map.replaceAll((k,v) -> v == null ? "" : v);
        return ChatHelper.replaceBy(line, start, end, ChatHelper.jsFix(_map));
    }
    public String applyToString(String line, char start, char end, String prefix, HashMap<String, String> map) {
        Map<String, String> _map = new HashMap<>();
        appendToReplace(new HashMap<>()).forEach((k,v) -> _map.put(prefix + k, v));
        _map.putAll(map);
        _map.replaceAll((k,v) -> v == null ? "" : v);
        return ChatHelper.replaceBy(line, start, end, ChatHelper.jsFix(_map));
    }
    public void init() {}

    @Override public String toString() { return appendToReplace(new HashMap<>()).entrySet().stream().map(v -> v.getKey() + ": " + v.getValue()).collect(Collectors.joining(", ")); }
}