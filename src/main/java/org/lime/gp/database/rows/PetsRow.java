package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.UUID;

import org.lime.gp.database.mysql.MySql;

public class PetsRow extends BaseRow {
    public int id;
    public UUID uuid;
    public String pet;
    public boolean isHide;
    public String name;
    public String color;

    public PetsRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        pet = MySql.readObject(set, "pet", String.class);
        isHide = MySql.readObject(set, "is_hide", Integer.class) != 0;
        name = MySql.readObject(set, "name", String.class);
        color = MySql.readObject(set, "color", String.class);
    }
    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("uuid", uuid.toString());
        map.put("pet", pet);
        map.put("is_hide", String.valueOf(isHide ? 1 : 0));
        map.put("name", name);
        map.put("color", color);
        return map;
    }
}