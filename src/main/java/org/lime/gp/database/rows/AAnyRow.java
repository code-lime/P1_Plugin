package org.lime.gp.database.rows;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;

import java.util.Map;
import java.util.UUID;

public class AAnyRow extends BaseRow {
    public int id;
    public UUID uuid;
    public String reason;
    public Integer timeToEnd;

    public AAnyRow(MySqlRow set) {
        super(set);
        id =  MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        reason = MySql.readObject(set, "reason", String.class);
        timeToEnd = MySql.readObject(set, "time", Integer.class);
    }
    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", id + "");
        map.put("uuid", uuid.toString());
        map.put("reason", reason);
        map.put("time", timeToEnd == null ? "null" : String.valueOf(timeToEnd));
        return map;
    }
}