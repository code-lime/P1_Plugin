package org.lime.gp.database.rows;

import java.util.Map;
import java.util.UUID;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;

public class PreDonateItemsRow extends BaseRow {
    public int id;
    public UUID uuid;
    public String type;
    public int amount;

    public PreDonateItemsRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        type = MySql.readObject(set, "type", String.class);
        amount = MySql.readObject(set, "amount", Integer.class);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("uuid", uuid.toString());
        map.put("type", type);
        map.put("amount", amount + "");
        return map;
    }
}