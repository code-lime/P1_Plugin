package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;

import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

public class Variable extends BaseRow {
    public double church;
    public Variable(ResultSet set) {
        super(set);
        church = MySql.readObject(set, "church", Double.class);
    }
    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
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