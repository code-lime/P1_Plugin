package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;

import org.lime.system;
import org.lime.gp.database.MySql;

public class PreDonateRow extends BaseRow {
    public enum State {
        NONE,
        GIVED,
        ERROR
    }
    public enum Type {
        DIAKA,
        TRADEMC,
        QIWI
    }
    public int id;
    public String name;
    public double amount;
    public PreDonateRow.Type type;
    public String info;
    public Calendar create_time;
    public PreDonateRow.State state;
    public PreDonateRow.State whitelist;

    public PreDonateRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        name = MySql.readObject(set, "name", String.class);
        amount = MySql.readObject(set, "amount", Double.class);
        type = Type.valueOf(MySql.readObject(set, "type", String.class));
        info = MySql.readObject(set, "info", String.class);
        create_time = MySql.readObject(set, "create_time", Calendar.class);
        state = State.valueOf(MySql.readObject(set, "state", String.class));
        whitelist = State.valueOf(MySql.readObject(set, "whitelist", String.class));
    }

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("name", name);
        map.put("amount", amount + "");
        map.put("type", type.name());
        map.put("info", info);
        map.put("create_time", system.formatCalendar(create_time, true));
        map.put("state", state.name());
        map.put("whitelist", whitelist.name());
        return map;
    }
}