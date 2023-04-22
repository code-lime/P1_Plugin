package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.UUID;

import org.lime.gp.database.mysql.MySql;

public class AAnyRow extends BaseRow {
    public int id;
    public UUID uuid;
    public String reason;
    public Integer timeToEnd;

    public AAnyRow(ResultSet set) {
        super(set);
        id =  MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        reason = MySql.readObject(set, "reason", String.class);
        timeToEnd = MySql.readObject(set, "time", Integer.class);
    }
    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", id + "");
        map.put("uuid", uuid.toString());
        map.put("reason", reason);
        map.put("time", timeToEnd == null ? "null" : String.valueOf(timeToEnd));
        return map;
    }
}