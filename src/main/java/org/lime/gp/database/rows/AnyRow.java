package org.lime.gp.database.rows;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;

import java.util.HashMap;
import java.util.Map;

public class AnyRow extends BaseRow {
    public HashMap<String, String> columns = new HashMap<>();
    public static AnyRow of(MySqlRow set) {
        return new AnyRow(set);
    }
    public AnyRow(MySqlRow set) {
        super(set);
        set.fields().forEach(kv -> columns.put(kv.column(), MySql.fromSqlObjectString(kv.value())));
    }
    public AnyRow(HashMap<String, String> columns) {
        super(null);
        this.columns.putAll(columns);
    }
    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        columns.forEach(map::put);
        return map;
    }
}