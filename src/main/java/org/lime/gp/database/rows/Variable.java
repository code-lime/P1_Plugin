package org.lime.gp.database.rows;

import java.util.Map;

import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;

public class Variable extends BaseRow {
    public double church;
    public Variable(MySqlRow set) {
        super(set);
        church = MySql.readObject(set, "church", Double.class);
    }
    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("church", String.valueOf(church));
        return map;
    }

    public static void addChurch(int add) {
        Methods.SQL.Async.rawSql("UPDATE variable SET church = church + @add", MySql.args().add("add", add).build(), () -> {});
    }

    public static Variable getSingle() {
        return Tables.VARIABLE_TABLE.getFirstRow().orElseThrow();
    }
}