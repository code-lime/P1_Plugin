package org.lime.gp.database.rows;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;
import org.lime.system.execute.Func1;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public class PrivateHouseRow extends BaseRow {
    public int id;
    public int houseId;
    public int patternId;
    public boolean status;

    public PrivateHouseRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        houseId = MySql.readObject(set, "house_id", Integer.class);
        patternId = MySql.readObject(set, "pattern_id", Integer.class);
        status = MySql.readObject(set, "status", Boolean.class);
    }

    public static List<PrivateHouseRow> getBy(Func1<PrivateHouseRow, Boolean> filter) {
        return Tables.PRIVATE_HOUSE.getRowsBy(filter);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("house_id", String.valueOf(houseId));
        map.put("pattern_id", String.valueOf(patternId));
        return map;
    }
}