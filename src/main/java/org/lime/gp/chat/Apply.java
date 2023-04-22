package org.lime.gp.chat;

import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.tables.ITable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Apply {
    private final Map<String, String> args;
    private final Map<String, ITable<? extends BaseRow>> tables;

    private Apply(Map<String, String> args, Map<String, ITable<? extends BaseRow>> tables) {
        this.args = args;
        this.tables = tables;
    }
    private Apply() {
        this(new HashMap<>(), new HashMap<>());
    }

    public static Apply of() { return new Apply(); }
    public Optional<ITable<? extends BaseRow>> getTable(String table) { return Optional.ofNullable(tables.get(table)); }

    public Apply add(String key, String value) {
        this.args.put(key, value);
        return this;
    }
    public Apply add(Map<String, String> args) {
        this.args.putAll(args);
        return this;
    }
    public Apply add(String prefix, String key, String value) {
        this.args.put(prefix + key, value);
        return this;
    }
    public Apply add(String prefix, Map<String, String> args) {
        args.forEach((k,v) -> this.args.put(prefix+k, v));
        return this;
    }
    public Apply add(BaseRow row) {
        return row == null ? this : add(row.appendToReplace(new HashMap<>()));
    }
    public Apply add(String prefix, BaseRow row) {
        return row == null ? this : add(prefix, row.appendToReplace(new HashMap<>()));
    }

    public Apply with(String key, ITable<? extends BaseRow> table) {
        this.tables.put(key, table);
        return this;
    }
    public Apply with(Map<String, ITable<? extends BaseRow>> tables) {
        this.tables.putAll(tables);
        return this;
    }

    public Apply join(Apply apply) {
        this.args.putAll(apply.args);
        this.tables.putAll(apply.tables);
        return this;
    }
    public String apply(String text) {
        return this.args.size() > 0 ? ChatHelper.replaceBy(text, '{', '}', ChatHelper.jsFix(this.args)) : text;
    }
    public Map<String, String> list() {
        return new HashMap<>(args);
    }
    public String getOrDefault(String key, String def) {
        return args.getOrDefault(key, def);
    }
    public boolean has(String key) { return args.containsKey(key); }
    public Optional<String> get(String key) {
        return Optional.ofNullable(args.get(key));
    }

    public Apply copy() {
        return new Apply(new HashMap<>(args), new HashMap<>(tables));
    }
}










