package org.lime.gp.database.rows;

import java.util.Map;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;

public class SmsPresetRow extends BaseRow {
    public int id;
    public String phone;
    public String text;

    public SmsPresetRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        phone = MySql.readObject(set, "phone", String.class);
        text = MySql.readObject(set, "text", String.class);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("phone", phone);
        map.put("text", text);
        return map;
    }
}