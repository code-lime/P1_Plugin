package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.lime.gp.database.mysql.MySql;

public class AnyRow extends BaseRow {
    public HashMap<String, String> columns = new HashMap<>();
    public static AnyRow of(ResultSet set) {
        return new AnyRow(set);
    }
    public AnyRow(ResultSet set) {
        super(set);
        try {
            ResultSetMetaData data = set.getMetaData();
            for (int i = 1; i <= data.getColumnCount(); i++)
                columns.put(data.getColumnLabel(i), MySql.fromSqlObjectString(data.getColumnType(i), set.getObject(i)));
        } catch (SQLException throwables) {
            throw new IllegalArgumentException(throwables);
        }
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