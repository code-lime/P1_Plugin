package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;

import org.lime.gp.database.mysql.MySql;

public class SmsPresetRow extends BaseRow {
    public int id;
    public String phone;
    public String text;

    public SmsPresetRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        phone = MySql.readObject(set, "phone", String.class);
        text = MySql.readObject(set, "text", String.class);
    }

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("phone", phone);
        map.put("text", text);
        return map;
    }
}