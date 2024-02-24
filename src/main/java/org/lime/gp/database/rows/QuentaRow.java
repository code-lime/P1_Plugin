package org.lime.gp.database.rows;

import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;

import java.util.Map;
import java.util.UUID;

public class QuentaRow extends BaseRow {
    public int id;
    public int userID;

    public QuentaRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        userID = MySql.readObject(set, "user_id", Integer.class);
    }

    public static boolean hasQuenta(int userID) { return Tables.QUENTA_TABLE.hasOther("user_id", String.valueOf(userID)); }
    public static boolean hasQuenta(UUID uuid) { return UserRow.getBy(uuid).map(v -> hasQuenta(v.id)).orElse(false); }

    public static boolean isQuentaEnable() { return Methods.isTableEnable("quenta"); }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("user_id", String.valueOf(userID));
        return map;
    }
}
