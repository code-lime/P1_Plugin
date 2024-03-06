package org.lime.gp.database.rows;

import java.util.Map;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;

public class HouseSubsRow extends BaseRow {
    public int id;
    public int userID;
    public int houseID;
    public int isOwner;

    public HouseSubsRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        userID = MySql.readObject(set, "user_id", Integer.class);
        houseID = MySql.readObject(set, "house_id", Integer.class);
        isOwner = MySql.readObject(set, "is_owner", Integer.class);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("user_id", String.valueOf(userID));
        map.put("house_id", String.valueOf(houseID));
        map.put("is_owner", String.valueOf(isOwner));
        return map;
    }
}