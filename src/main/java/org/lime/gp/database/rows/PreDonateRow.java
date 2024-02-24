package org.lime.gp.database.rows;

import java.util.Calendar;
import java.util.Map;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.system.Time;

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
    public Calendar createTime;
    public PreDonateRow.State state;
    public PreDonateRow.State whitelist;

    public PreDonateRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        name = MySql.readObject(set, "name", String.class);
        amount = MySql.readObject(set, "amount", Double.class);
        type = Type.valueOf(MySql.readObject(set, "type", String.class));
        info = MySql.readObject(set, "info", String.class);
        createTime = MySql.readObject(set, "create_time", Calendar.class);
        state = State.valueOf(MySql.readObject(set, "state", String.class));
        whitelist = State.valueOf(MySql.readObject(set, "whitelist", String.class));
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("name", name);
        map.put("amount", String.valueOf(amount));
        map.put("type", type.name());
        map.put("info", info);
        map.put("create_time", Time.formatCalendar(createTime, true));
        map.put("state", state.name());
        map.put("whitelist", whitelist.name());
        return map;
    }
}