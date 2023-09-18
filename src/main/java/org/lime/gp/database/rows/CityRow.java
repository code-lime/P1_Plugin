package org.lime.gp.database.rows;

import org.bukkit.util.Vector;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Optional;

public class CityRow extends BaseRow {
    public int id;
    public Optional<Vector> posMain;

    public CityRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        posMain = Methods.readPositionOptional(set, "pos_main");
    }

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        return map;
    }

    public static Optional<CityRow> getBy(int id) { return Tables.CITY_TABLE.get(String.valueOf(id)); }
}