package org.lime.gp.database.rows;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;

public class UserFlagsRow extends BaseRow {
    public int id;
    public UUID uuid;
    public int backPackID;

    public UserFlagsRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        backPackID = MySql.readObject(set, "backpack_id", Integer.class);
    }

    public static Optional<Integer> backPackID(UUID uuid) {
        return Tables.USERFLAGS_TABLE.getOther("uuid", uuid.toString()).map(v -> v.backPackID);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("uuid", uuid.toString());
        map.put("backpack_id", String.valueOf(backPackID));
        return map;
    }
}